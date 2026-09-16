package sh4dow18.miteve_api.services.scrape

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.scrape.MediafireDownloadInfo
import sh4dow18.miteve_api.dtos.scrape.MediafireResolveResponse
import sh4dow18.miteve_api.dtos.scrape.ScrapeResponse
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.services.scrape.fallback.AnimeFallbackService
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
@Service
class LamovieScrapeService(
    @Value("\${tmdb.api-key:}") private val tmdbApiKeyProp: String,
    @Value("\${lamovie.base-url:https://lamovie.org}") private val lamovieBaseUrl: String,
    @Value("\${lamovie.fast-api:https://lamovie.org/wp-api/v1}") private val lamovieFastApi: String,
    @Value("\${lamovie.la.base-url:https://lamovie.la}") private val lamovieLaBaseUrl: String,
    @Value("\${lamovie.la.fast-api:https://lamovie.la/wp-api/v1}") private val lamovieLaFastApi: String,
    @Value("\${lamovie.download-dir:./downloads}") private val downloadDir: String,
    @Autowired(required = false) private val fallbackService: AnimeFallbackService? = null
) : AbstractLamovieScrapeService {

    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val mapper = ObjectMapper()

    private val fallbackTmdbKeys = listOf(
        "10923b261ba94d897ac6b81148314a3f",
        "b573d051ec65413c949e68169923f7ff",
        "da40aaeca884d8c9a9a4c088917c474c",
        "4e44d9029b1270a757cddc766a1bcb63",
        "39151834c95219c3cae772b4465079d7",
        "6bca0b74270a3299673d934c1bb11b4d",
        "902ddd650dd51f569c2ef95468612ad1",
        "4c7ff8e6151131469216f007e4be3b3d",
        "21e3f055fa996f78a2886737bb6e7957",
        "98325a9d3ed3ec225e41ccc4d360c817",
        "3fd2be6f0c70a2a598f084ddfb75487c",
        "9780d3ceee590a40bd3446da3f81171d",
        "04c35731a5ee918f014970082a0088b1",
        "516adf1e1567058f8ecbf30bf2eb9378",
        "9b702a6b89b0278738dab62417267c49"
    )

    override fun scrape(tmdbId: String, type: String, season: Int?, episode: Int?): ScrapeResponse {
        if (!tmdbId.matches(Regex("^\\d+$"))) {
            return scrapeBySlug(tmdbId, type, season, episode)
        }
        val normalizedType = normalizeType(type)
        val tmdbInfo = fetchTmdbInfo(tmdbId, normalizedType)
        val tmdbTitle = tmdbInfo.first
        val tmdbYear = tmdbInfo.second
        val searchPosts = searchLamovie(tmdbTitle)
        if (searchPosts.isEmpty()) {
            // Ningún resultado en lamovie -> intenta fallback directo para anime
            if (isAnimeCandidate(normalizedType)) {
                val attempted = mutableListOf<String>("lamovie")
                val fallback = fallbackService?.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, tmdbTitle, "animes", season, episode, attempted)
                if (fallback != null) {
                    return fallback.copy(attemptedProviders = attempted)
                }
            }
            throw NoExists(tmdbId, "LaMovie content for title $tmdbTitle")
        }
        val best = pickBestPost(searchPosts, tmdbTitle, tmdbYear, normalizedType)
            ?: searchPosts[0]
        val lamovieId = best.get("_id").asLong()
        val lamovieSlug = best.get("slug").asText()
        val lamovieTitle = best.get("title").asText()
        val lamovieType = best.get("type").asText()
        val lamovieUrl = buildLamovieUrl(lamovieSlug, lamovieType)
        var targetPostId = lamovieId
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = try {
            fetchPlayer(targetPostId)
        } catch (e: Exception) {
            // Si es anime y falla player en lamovie, intenta fallback antes de fallar
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw e
        }
        val downloadsNode = player.get("downloads")
        if (downloadsNode == null || !downloadsNode.isArray || downloadsNode.size() == 0) {
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw NoExists("$targetPostId", "Downloads (modal vacío) for $lamovieTitle")
        }
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText())
            u.lowercase().contains("mediafire.com")
        }
        val latinoCandidates = mediafireCandidates.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }

        // ========== ANIME: SOLO LATINO (nunca japonés) ==========
        // Si es anime, se ignora lamovie.la cuando solo hay japonés y se fuerza fallback latino.
        // Flujo: lamovie.la (se busca latino) -> JKanime -> MonosChinos -> AnimeFLV -> Consumet
        if (isAnimeType(lamovieType)) {
            if (latinoCandidates.isNotEmpty()) {
                // Hay latino en lamovie.la, úsalo directamente (no fallback)
                val mediafireNodeLatino = latinoCandidates.minByOrNull { priority(it) }!!
                val mediafireUrl = if (mediafireNodeLatino.has("url_raw")) mediafireNodeLatino.get("url_raw").asText() else mediafireNodeLatino.get("url").asText()
                val resolved = try {
                    resolveMediafire(mediafireUrl)
                } catch (e: Exception) {
                    if (fallbackService?.isFallbackEnabled() == true) {
                        val attempted = mutableListOf<String>("lamovie")
                        val fb = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                        if (fb != null) return fb.copy(attemptedProviders = attempted)
                    }
                    throw e
                }
                val quality = mediafireNodeLatino.get("quality")?.takeIf { !it.isNull }?.asText()
                val lang = mediafireNodeLatino.get("lang")?.takeIf { !it.isNull }?.asText()
                val size = mediafireNodeLatino.get("size")?.takeIf { !it.isNull }?.asText()
                @Suppress("UNCHECKED_CAST")
                val allDownloads = downloadsList.map { n -> mapper.convertValue(n, Map::class.java) as Map<String, Any?> }
                return ScrapeResponse(
                    tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
                    lamovieId = lamovieId, lamovieSlug = lamovieSlug, lamovieTitle = lamovieTitle, lamovieUrl = lamovieUrl, lamovieType = lamovieType,
                    season = seasonNum, episode = episodeNum, episodeId = episodeId,
                    mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, quality, lang, size),
                    allDownloads = allDownloads, source = "lamovie", sourceUrl = lamovieUrl, fallbackUsed = false, fallbackReason = null, attemptedProviders = null
                )
            } else {
                // No hay latino en lamovie.la (solo japonés / vacío) -> IGNORAR lamovie y buscar fallback latino SIEMPRE
                if (fallbackService?.isFallbackEnabled() == true) {
                    val attempted = mutableListOf<String>("lamovie")
                    val fallback = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                    if (fallback != null) {
                        return fallback.copy(attemptedProviders = attempted)
                    }
                    // Fallback no encontró latino -> NO devolver japonés, lanzar error claro
                    throw NoExists("$targetPostId", "No se encontró versión en español latino para '$lamovieTitle' (lamovie.la solo tiene japonés: ${mediafireCandidates.size} mediafire japonés, 0 latino). Fallback intentó: ${attempted.joinToString(",")} sin éxito. Solo se permite latino para anime (Konosuba 2016 y similares). Intenta con JKanime/MonosChinos/AnimeFLV manualmente o reporta el anime.")
                } else {
                    throw NoExists("$targetPostId", "No hay latino en lamovie.la para '$lamovieTitle' (solo japonés) y fallback deshabilitado. Solo español latino permitido para anime.")
                }
            }
        }

        // ========== NO-ANIME: lógica original (película/serie no anime) ==========
        if (mediafireCandidates.isEmpty() && fallbackService?.isFallbackEnabled() == true) {
            // Para no-anime también intentamos fallback si no hay mediafire, pero no es estricto
            val attempted = mutableListOf<String>("lamovie")
            val fallback = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
            if (fallback != null) return fallback.copy(attemptedProviders = attempted)
        }

        val mediafireNode = mediafireCandidates.minByOrNull { priority(it) } ?: throw NoExists("$targetPostId", "Mediafire download card")
        val mediafireUrl = if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText()
        val resolved = try {
            resolveMediafire(mediafireUrl)
        } catch (e: Exception) {
            throw e
        }
        val quality = mediafireNode.get("quality")?.takeIf { !it.isNull }?.asText()
        val lang = mediafireNode.get("lang")?.takeIf { !it.isNull }?.asText()
        val size = mediafireNode.get("size")?.takeIf { !it.isNull }?.asText()
        @Suppress("UNCHECKED_CAST")
        val allDownloads = downloadsList.map { n ->
            mapper.convertValue(n, Map::class.java) as Map<String, Any?>
        }
        return ScrapeResponse(
            tmdbId = tmdbId,
            type = normalizedType,
            tmdbTitle = tmdbTitle,
            tmdbYear = tmdbYear,
            lamovieId = lamovieId,
            lamovieSlug = lamovieSlug,
            lamovieTitle = lamovieTitle,
            lamovieUrl = lamovieUrl,
            lamovieType = lamovieType,
            season = seasonNum,
            episode = episodeNum,
            episodeId = episodeId,
            mediafire = MediafireDownloadInfo(
                mediafireUrl = mediafireUrl,
                directUrl = resolved.directUrl,
                filename = resolved.filename,
                quality = quality,
                lang = lang,
                size = size
            ),
            allDownloads = allDownloads,
            source = "lamovie",
            sourceUrl = lamovieUrl,
            fallbackUsed = false,
            fallbackReason = null,
            attemptedProviders = null
        )
    }

    override fun scrapeBySlug(slug: String, type: String, season: Int?, episode: Int?): ScrapeResponse {
        val normalizedType = normalizeType(type)
        val postType = if (normalizedType == "movie") "movies" else "tvshows"
        val single = try {
            fetchSingleBySlug(slug, postType)
        } catch (e: Exception) {
            // Si lamovie no tiene el slug y es posible anime, intenta fallback por slug
            if (isAnimeCandidate(normalizedType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw e
        }
        val lamovieId = single.get("_id").asLong()
        val lamovieSlug = single.get("slug").asText()
        val lamovieTitle = single.get("title").asText()
        val lamovieType = single.get("type").asText()
        val lamovieUrl = buildLamovieUrl(lamovieSlug, lamovieType)
        var targetPostId = lamovieId
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = try {
            fetchPlayer(targetPostId)
        } catch (e: Exception) {
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw e
        }
        val downloadsNode = player.get("downloads")
        if (downloadsNode == null || !downloadsNode.isArray || downloadsNode.size() == 0) {
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw NoExists("$targetPostId", "Downloads (modal vacío) for $lamovieTitle")
        }
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText())
            u.lowercase().contains("mediafire.com")
        }
        val latinoCandidates = mediafireCandidates.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }

        // ========== ANIME SLUG: SOLO LATINO (nunca japonés) ==========
        if (isAnimeType(lamovieType)) {
            if (latinoCandidates.isNotEmpty()) {
                val mediafireNodeLatino = latinoCandidates.minByOrNull { priority(it) }!!
                val mediafireUrl = if (mediafireNodeLatino.has("url_raw")) mediafireNodeLatino.get("url_raw").asText() else mediafireNodeLatino.get("url").asText()
                val resolved = try {
                    resolveMediafire(mediafireUrl)
                } catch (e: Exception) {
                    if (fallbackService?.isFallbackEnabled() == true) {
                        val attempted = mutableListOf<String>("lamovie")
                        val fb = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                        if (fb != null) return fb.copy(attemptedProviders = attempted)
                    }
                    throw e
                }
                val quality = mediafireNodeLatino.get("quality")?.takeIf { !it.isNull }?.asText()
                val lang = mediafireNodeLatino.get("lang")?.takeIf { !it.isNull }?.asText()
                val size = mediafireNodeLatino.get("size")?.takeIf { !it.isNull }?.asText()
                @Suppress("UNCHECKED_CAST")
                val allDownloads = downloadsList.map { n -> mapper.convertValue(n, Map::class.java) as Map<String, Any?> }
                val displayTmdbId = single.get("tmdbId")?.asText() ?: slug
                return ScrapeResponse(
                    tmdbId = displayTmdbId, type = normalizedType, tmdbTitle = lamovieTitle, tmdbYear = null,
                    lamovieId = lamovieId, lamovieSlug = lamovieSlug, lamovieTitle = lamovieTitle, lamovieUrl = lamovieUrl, lamovieType = lamovieType,
                    season = seasonNum, episode = episodeNum, episodeId = episodeId,
                    mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, quality, lang, size),
                    allDownloads = allDownloads, source = "lamovie", sourceUrl = lamovieUrl, fallbackUsed = false, fallbackReason = null, attemptedProviders = null
                )
            } else {
                // No hay latino en lamovie.la por slug -> IGNORAR japonés y forzar fallback latino
                if (fallbackService?.isFallbackEnabled() == true) {
                    val attempted = mutableListOf<String>("lamovie")
                    val fallback = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                    if (fallback != null) return fallback.copy(attemptedProviders = attempted)
                    throw NoExists(slug, "No se encontró versión en español latino para slug '$slug' (lamovie.la solo tiene japonés: ${mediafireCandidates.size} mediafire japonés, 0 latino). Fallback intentó: ${attempted.joinToString(",")} sin éxito. Solo latino permitido para anime.")
                } else {
                    throw NoExists(slug, "No hay latino en lamovie.la para slug '$slug' (solo japonés) y fallback deshabilitado. Solo español latino para anime.")
                }
            }
        }

        // ========== NO-ANIME SLUG: lógica original ==========
        val mediafireNode = mediafireCandidates.minByOrNull { priority(it) } ?: throw NoExists("$targetPostId", "Mediafire download card")
        val mediafireUrl = if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText()
        val resolved = try {
            resolveMediafire(mediafireUrl)
        } catch (e: Exception) {
            throw e
        }
        val quality = mediafireNode.get("quality")?.takeIf { !it.isNull }?.asText()
        val lang = mediafireNode.get("lang")?.takeIf { !it.isNull }?.asText()
        val size = mediafireNode.get("size")?.takeIf { !it.isNull }?.asText()
        @Suppress("UNCHECKED_CAST")
        val allDownloads = downloadsList.map { n -> mapper.convertValue(n, Map::class.java) as Map<String, Any?> }
        val displayTmdbId = single.get("tmdbId")?.asText() ?: slug
        return ScrapeResponse(
            tmdbId = displayTmdbId,
            type = normalizedType,
            tmdbTitle = lamovieTitle,
            tmdbYear = null,
            lamovieId = lamovieId,
            lamovieSlug = lamovieSlug,
            lamovieTitle = lamovieTitle,
            lamovieUrl = lamovieUrl,
            lamovieType = lamovieType,
            season = seasonNum,
            episode = episodeNum,
            episodeId = episodeId,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, quality, lang, size),
            allDownloads = allDownloads,
            source = "lamovie",
            sourceUrl = lamovieUrl,
            fallbackUsed = false,
            fallbackReason = null,
            attemptedProviders = null
        )
    }

    private fun fetchSingleBySlug(slug: String, postType: String): JsonNode {
        val candidates = when (postType) {
            "movies" -> listOf("movies", "tvshows", "animes")
            "tvshows" -> listOf("tvshows", "animes", "movies")
            else -> listOf(postType, "movies", "tvshows", "animes")
        }.distinct()
        var lastError: Exception? = null
        for (pt in candidates) {
            val fastApis = if (pt == "animes") listOf(lamovieLaFastApi, lamovieFastApi) else listOf(lamovieFastApi, lamovieLaFastApi)
            for (fastApi in fastApis) {
                try {
                    val url = "$fastApi/single/$pt?slug=$slug&postType=$pt"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: continue
                if (data.isArray && data.size() == 0) continue
                if (data.has("error") && data.get("error").asBoolean()) continue
                return data
            } catch (e: NoExists) {
                lastError = e
                if (e.message?.contains("HTTP 404") == true) continue
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        }
        val fallbackCandidates = mutableListOf<String>()
        val baseSearch = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim().replace(Regex("\\s+"), " ")
        fallbackCandidates.add(baseSearch)
        baseSearch.split(Regex("\\s+")).filter { it.length >= 4 }.forEach { fallbackCandidates.add(it) }
        fallbackCandidates.add(slug.substringBefore("-"))
        for (q in fallbackCandidates.distinct().filter { it.length >= 3 }.take(5)) {
            try {
                val searchResults = searchLamovie(q)
                if (searchResults.isNotEmpty()) {
                    val best = pickBestPost(searchResults, baseSearch, null, if (postType == "movies") "movie" else "tv")
                    if (best != null) return best
                    return searchResults[0]
                }
            } catch (_: Exception) {}
        }
        throw lastError ?: NoExists(slug, "LaMovie slug $postType")
    }

    override fun resolveMediafire(mediafireUrl: String): MediafireResolveResponse {
        if (!mediafireUrl.lowercase().contains("mediafire.com")) throw BadRequest("URL no es de mediafire: $mediafireUrl")
        val body = httpGetString(mediafireUrl, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
        var direct = Regex("""aria-label="Download file"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""id="downloadButton"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""href="(https://download[^"]+mediafire[^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""href="(https://download[^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) throw NoExists(mediafireUrl, "Mediafire direct link")
        direct = direct.replace("&amp;", "&")
        val filename = try {
            val uri = URI(direct)
            uri.path.substringAfterLast("/").takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
        return MediafireResolveResponse(mediafireUrl = mediafireUrl, directUrl = direct, filename = filename)
    }

    override fun getDirectDownloadStream(mediafireUrl: String): Pair<String, java.io.InputStream> {
        val resolved = resolveMediafire(mediafireUrl)
        val directUrl = resolved.directUrl
        val request = HttpRequest.newBuilder(URI(directUrl)).GET()
            .header("User-Agent", "Mozilla/5.0")
            .timeout(Duration.ofSeconds(60))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) throw BadRequest("Error al descargar de Mediafire: HTTP ${response.statusCode()}")
        val filename = resolved.filename ?: "download"
        return filename to response.body()
    }

    override fun downloadToServer(tmdbId: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        if (!tmdbId.matches(Regex("^\\d+$"))) {
            return downloadBySlugToServer(tmdbId, type, season, episode)
        }
        try {
            val info = scrape(tmdbId, type, season, episode)
            return downloadResolvedToServer(info.mediafire.directUrl, info.mediafire.filename, info)
        } catch (e: NoExists) {
            if (e.message?.contains("Mediafire direct link") == true || e.message?.contains("HTTP 404") == true && e.message?.contains("mediafire.com") == true) {
                return downloadTorrentFallback(tmdbId, type, season, episode)
            }
            throw e
        } catch (e: BadRequest) {
            if (e.message?.contains("HTTP 404") == true && e.message?.contains("mediafire.com") == true) {
                return downloadTorrentFallback(tmdbId, type, season, episode)
            }
            throw e
        }
    }

    override fun downloadBySlugToServer(slug: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        try {
            val info = scrapeBySlug(slug, type, season, episode)
            return downloadResolvedToServer(info.mediafire.directUrl, info.mediafire.filename, info)
        } catch (e: NoExists) {
            if (e.message?.contains("Mediafire direct link") == true || e.message?.contains("HTTP 404") == true && e.message?.contains("mediafire.com") == true) {
                return downloadTorrentFallbackBySlug(slug, type, season, episode)
            }
            throw e
        } catch (e: BadRequest) {
            if (e.message?.contains("HTTP 404") == true && e.message?.contains("mediafire.com") == true) {
                return downloadTorrentFallbackBySlug(slug, type, season, episode)
            }
            throw e
        }
    }

    private fun downloadTorrentFallbackBySlug(slug: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val normalizedType = normalizeType(type)
        val postType = if (normalizedType == "movie") "movies" else "tvshows"
        val single = fetchSingleBySlug(slug, postType)
        val lamovieId = single.get("_id").asLong()
        val lamovieSlug = single.get("slug").asText()
        val lamovieTitle = single.get("title").asText()
        val lamovieType = single.get("type").asText()
        val lamovieUrl = buildLamovieUrl(lamovieSlug, lamovieType)
        var targetPostId = lamovieId
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        var episodeId: Long? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = fetchPlayer(targetPostId)
        val downloadsNode = player.get("downloads") ?: throw NoExists("$targetPostId", "Downloads torrent fallback slug")
        val downloadsList = downloadsNode.toList()
        val torrentCandidatesSlug = downloadsList.filter { n -> (n.get("quality")?.asText()?.lowercase() ?: "").contains("1080p") }
            .filter { n -> val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase(); u.startsWith("magnet:") || u.contains(".torrent") }
        val latinoTorrentCandidatesSlug = torrentCandidatesSlug.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }
        // Para anime: SOLO latino, nunca torrent japonés
        if (isAnimeType(lamovieType) && latinoTorrentCandidatesSlug.isEmpty()) {
            throw NoExists("$targetPostId", "No se encontró torrent latino 1080p para '$lamovieTitle' (solo japonés). Solo latino permitido para anime.")
        }
        val poolTorrentSlug = if (isAnimeType(lamovieType)) latinoTorrentCandidatesSlug else if (latinoTorrentCandidatesSlug.isNotEmpty()) latinoTorrentCandidatesSlug else torrentCandidatesSlug
        val torrentNode = poolTorrentSlug.minByOrNull { n -> val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase(); if (u.contains(".torrent")) 0 else 1 }
            ?: throw NoExists("$targetPostId", "Torrent 1080p not found slug")
        val torrentUrl = if (torrentNode.has("url_raw")) torrentNode.get("url_raw").asText() else torrentNode.get("url").asText()
        val scrapeInfo = ScrapeResponse(slug, normalizedType, lamovieTitle, null, lamovieId, lamovieSlug, lamovieTitle, lamovieUrl, lamovieType, seasonNum, episodeNum, episodeId, MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", null, null), null)
        return downloadTorrentToServer(torrentUrl, scrapeInfo)
    }

    fun downloadTorrentFallback(tmdbId: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        return downloadTorrentFallbackInternal(tmdbId, type, season, episode)
    }

    internal fun downloadTorrentFallbackInternal(tmdbId: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val normalizedType = normalizeType(type)
        val tmdbInfo = fetchTmdbInfo(tmdbId, normalizedType)
        val searchPosts = searchLamovie(tmdbInfo.first)
        if (searchPosts.isEmpty()) throw NoExists(tmdbId, "LaMovie content for torrent fallback")
        val best = pickBestPost(searchPosts, tmdbInfo.first, tmdbInfo.second, normalizedType) ?: searchPosts[0]
        val lamovieId = best.get("_id").asLong()
        val lamovieSlug = best.get("slug").asText()
        val lamovieTitle = best.get("title").asText()
        val lamovieType = best.get("type").asText()
        val lamovieUrl = buildLamovieUrl(lamovieSlug, lamovieType)
        var targetPostId = lamovieId
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        var episodeId: Long? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = fetchPlayer(targetPostId)
        val downloadsNode = player.get("downloads") ?: throw NoExists("$targetPostId", "Downloads torrent fallback")
        val downloadsList = downloadsNode.toList()
        val torrentCandidates = downloadsList.filter { n ->
            val q = n.get("quality")?.asText()?.lowercase() ?: ""
            q.contains("1080p")
        }.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase()
            u.startsWith("magnet:") || u.contains(".torrent") || u.contains("magnet")
        }
        val latinoTorrentCandidates = torrentCandidates.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }
        if (isAnimeType(lamovieType) && latinoTorrentCandidates.isEmpty()) {
            throw NoExists("$targetPostId", "No se encontró torrent latino 1080p para '$lamovieTitle' (solo japonés). Solo latino permitido para anime.")
        }
        val poolTorrent = if (isAnimeType(lamovieType)) latinoTorrentCandidates else if (latinoTorrentCandidates.isNotEmpty()) latinoTorrentCandidates else torrentCandidates
        val torrentNode = poolTorrent.minByOrNull { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase()
            if (u.contains(".torrent")) 0 else 1
        } ?: throw NoExists("$targetPostId", "Torrent 1080p not found")
        val torrentUrl = if (torrentNode.has("url_raw")) torrentNode.get("url_raw").asText() else torrentNode.get("url").asText()
        val scrapeInfo = ScrapeResponse(
            tmdbId = tmdbId,
            type = normalizedType,
            tmdbTitle = tmdbInfo.first,
            tmdbYear = tmdbInfo.second,
            lamovieId = lamovieId,
            lamovieSlug = lamovieSlug,
            lamovieTitle = lamovieTitle,
            lamovieUrl = lamovieUrl,
            lamovieType = lamovieType,
            season = seasonNum,
            episode = episodeNum,
            episodeId = episodeId,
            mediafire = MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", null, null),
            allDownloads = null
        )
        return downloadTorrentToServer(torrentUrl, scrapeInfo)
    }

    private fun downloadTorrentToServer(torrentUrl: String, scrapeInfo: ScrapeResponse): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val baseDir = Paths.get(downloadDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
        val folderSlug = getDownloadFolderSlug(scrapeInfo)
        val (finalTarget, filename) = if (scrapeInfo.season != null && scrapeInfo.episode != null) {
            val seasonDir = baseDir.resolve(folderSlug).resolve("season-${scrapeInfo.season}")
            Files.createDirectories(seasonDir)
            val f = "episode-${scrapeInfo.episode}.mp4"
            seasonDir.resolve(f) to f
        } else {
            val movieFile = "$folderSlug.mp4"
            baseDir.resolve(movieFile) to movieFile
        }
        val normalizedTarget = finalTarget.normalize()
        if (!normalizedTarget.startsWith(baseDir)) throw BadRequest("Nombre torrent inválido")
        val torrentName = if (scrapeInfo.season != null) "episode-${scrapeInfo.episode}" else folderSlug
        val contentFile = downloadTorrentContent(torrentUrl, torrentName, scrapeInfo)
        try {
            Files.deleteIfExists(normalizedTarget)
            Files.move(Paths.get(contentFile.absolutePath), normalizedTarget)
        } catch (e: Exception) {
            try { contentFile.copyTo(normalizedTarget.toFile(), overwrite = true); contentFile.delete() } catch (_: Exception) { throw BadRequest("Error moviendo contenido torrent: ${e.message}") }
        }
        try { Paths.get(contentFile.absolutePath).parent.toFile().deleteRecursively() } catch (_: Exception) {}
        val fileSize = try { Files.size(normalizedTarget) } catch (_: Exception) { null }
        return sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse(
            success = true,
            message = "Contenido torrent 1080p descargado con nombre $filename (fallback mediafire 404)",
            tmdbId = scrapeInfo.tmdbId,
            type = scrapeInfo.type,
            lamovieUrl = scrapeInfo.lamovieUrl,
            lamovieTitle = scrapeInfo.lamovieTitle,
            mediafireUrl = torrentUrl,
            directUrl = torrentUrl,
            filename = filename,
            filePath = normalizedTarget.toString(),
            fileSize = fileSize,
            season = scrapeInfo.season,
            episode = scrapeInfo.episode
        )
    }

    override fun downloadMediafireToServer(mediafireUrl: String): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val resolved = resolveMediafire(mediafireUrl)
        return downloadResolvedToServer(resolved.directUrl, resolved.filename, null, mediafireUrl)
    }

    private fun downloadResolvedToServer(directUrl: String, filenameHint: String?, scrapeInfo: ScrapeResponse? = null, originalMediafireUrl: String? = null): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val baseDir = Paths.get(downloadDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
        // Para fallbacks latinos (latanime, jkanime, etc.) el mediafire ya es .mp4 directo, no .rar
        val isMp4Direct = directUrl.lowercase().contains(".mp4")
        val isMkvDirect = directUrl.lowercase().contains(".mkv")
        val isStream = directUrl.contains(".m3u8") || scrapeInfo?.source in setOf("consumet", "animeflv", "latanime", "jkanime", "monoschinos", "tioanime", "animefenix", "nyaa") && (isMp4Direct || isMkvDirect)
        val extension = when {
            directUrl.contains(".m3u8") -> ".m3u8"
            isMp4Direct -> ".mp4"
            isMkvDirect -> ".mkv"
            isStream -> ".mp4"
            else -> ".rar"
        }
        val (target, filename) = if (scrapeInfo != null && scrapeInfo.season != null && scrapeInfo.episode != null) {
            val folderSlug = getDownloadFolderSlug(scrapeInfo)
            val seasonDir = baseDir.resolve(folderSlug).resolve("season-${scrapeInfo.season}")
            Files.createDirectories(seasonDir)
            val f = "episode-${scrapeInfo.episode}$extension"
            seasonDir.resolve(f) to f
        } else if (scrapeInfo != null) {
            val folderSlug = getDownloadFolderSlug(scrapeInfo)
            val movieFile = "$folderSlug$extension"
            baseDir.resolve(movieFile) to movieFile
        } else {
            val hintRaw = sanitizeFilename(filenameHint ?: directUrl.substringAfterLast("/").substringBefore("?").ifBlank { "download$extension" })
            val hint = if (isStream && hintRaw.endsWith(".rar")) hintRaw.removeSuffix(".rar") + extension else hintRaw
            baseDir.resolve(hint) to hint
        }
        val normalizedTarget = target.normalize()
        if (!normalizedTarget.startsWith(baseDir)) throw BadRequest("Nombre de archivo inválido")
        // Intenta descarga con HttpClient + headers de navegador, con fallback a curl para Cloudflare/mediafire
        var lastStatus = -1
        var lastError: Exception? = null
        try {
            val request = HttpRequest.newBuilder(URI(directUrl)).GET()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Referer", scrapeInfo?.lamovieUrl ?: "https://www.google.com/")
                .timeout(Duration.ofMinutes(10))
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofFile(normalizedTarget))
            lastStatus = response.statusCode()
            if (response.statusCode() in 200..299) {
                // éxito
            } else {
                try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                // Fallback a curl si HttpClient da 404/403 (mediafire a veces bloquea HttpClient)
                println("[Download] HttpClient HTTP $lastStatus para $directUrl, intenta curl fallback")
                throw BadRequest("HTTP $lastStatus")
            }
        } catch (e: Exception) {
            lastError = e
            if (e.message?.contains("HTTP 404") == true || e.message?.contains("HTTP 403") == true || lastStatus in setOf(404,403,429,520)) {
                // Fallback via curl -L
                try {
                    println("[Download] Fallback curl para $directUrl (scrape source=${scrapeInfo?.source})")
                    val curlCmd = arrayOf("curl", "-s", "-L", "-A", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", "-o", normalizedTarget.toString(), directUrl)
                    val pb = ProcessBuilder(*curlCmd).redirectErrorStream(true)
                    val proc = pb.start()
                    val finished = proc.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)
                    if (!finished) { proc.destroyForcibly(); throw BadRequest("Timeout curl para $directUrl") }
                    if (proc.exitValue() != 0) {
                        val out = proc.inputStream.bufferedReader().readText().take(500)
                        throw BadRequest("curl fallo ${proc.exitValue()} para $directUrl: $out")
                    }
                    if (!Files.exists(normalizedTarget) || Files.size(normalizedTarget) == 0L) {
                        throw BadRequest("curl descargó vacío para $directUrl")
                    }
                    lastStatus = 200
                    lastError = null
                } catch (curlE: Exception) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $directUrl (curl fallback falló: ${curlE.message}, orig: ${e.message}, source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl})")
                }
            } else if (lastStatus == -1) {
                try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                throw BadRequest("Error al descargar en servidor: ${e.message} para $directUrl (source=${scrapeInfo?.source})")
            } else {
                try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $directUrl (source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl})")
            }
        }
        if (lastStatus !in 200..299 && lastError == null) {
            // Si curl tuvo éxito, lastStatus ya es 200
        } else if (lastStatus !in 200..299) {
            throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $directUrl")
        }
        val fileSize = try { Files.size(normalizedTarget) } catch (_: Exception) { null }
        // Verifica que no sea HTML (37.5kB indica página HTML, no video)
        try {
            if (fileSize != null && fileSize < 5 * 1024 * 1024) {
                val head = Files.readAllBytes(normalizedTarget).take(2048).toByteArray().toString(Charsets.UTF_8).lowercase()
                if (head.contains("<html") || head.contains("<!doctype") || head.contains("<head")) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Descarga resultó en HTML (${fileSize} bytes) en vez de video para $directUrl (source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl}, episode S${scrapeInfo?.season}E${scrapeInfo?.episode}). Posible mediafire directo expirado o latanime bloqueado. Intenta de nuevo o reporta.")
                }
                // También si es muy pequeño y no es video esperado (episodio debe ser >50MB)
                if (fileSize < 50 * 1024 && head.contains("mediafire") ) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Archivo muy pequeño (${fileSize} bytes) parece página mediafire, no video. DirectUrl: $directUrl")
                }
            }
        } catch (e: BadRequest) { throw e } catch (_: Exception) { /* ignora check si falla lectura */ }
        val msg = when {
            scrapeInfo?.fallbackUsed == true && scrapeInfo.source != "lamovie" -> "Descargado via fallback ${scrapeInfo.source} (latino) correctamente en el servidor"
            scrapeInfo?.source == "consumet" -> "Stream HLS descargado via consumet"
            else -> "Descargado correctamente en el servidor"
        }
        return sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse(
            success = true,
            message = msg,
            tmdbId = scrapeInfo?.tmdbId ?: "",
            type = scrapeInfo?.type ?: "mediafire",
            lamovieUrl = scrapeInfo?.lamovieUrl ?: "",
            lamovieTitle = scrapeInfo?.lamovieTitle ?: filename,
            mediafireUrl = scrapeInfo?.mediafire?.mediafireUrl ?: originalMediafireUrl ?: "",
            directUrl = directUrl,
            filename = filename,
            filePath = normalizedTarget.toString(),
            fileSize = fileSize,
            season = scrapeInfo?.season,
            episode = scrapeInfo?.episode,
            source = scrapeInfo?.source ?: "lamovie",
            fallbackUsed = scrapeInfo?.fallbackUsed ?: false
        )
    }

    private fun downloadTorrentContent(torrentUrl: String, baseName: String, scrapeInfo: ScrapeResponse): File {
        val tempDir = Files.createTempDirectory("torrent-$baseName-").toFile()
        try {
            val isMagnet = torrentUrl.lowercase().startsWith("magnet:")
            val isTorrentHttp = torrentUrl.lowercase().contains(".torrent")
            if (!isCommandAvailable("aria2c")) throw BadRequest("aria2c no instalado para descargar torrent/magnet. Instala: sudo apt-get install -y aria2")
            val cmd = when {
                isMagnet -> arrayOf("aria2c", "--seed-time=0", "--dir=${tempDir.absolutePath}", "--allow-overwrite=true", "--auto-file-renaming=false", torrentUrl)
                isTorrentHttp -> arrayOf("aria2c", "--seed-time=0", "--dir=${tempDir.absolutePath}", "--allow-overwrite=true", torrentUrl)
                else -> throw BadRequest("URL torrent no soportada: $torrentUrl")
            }
            val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(60, TimeUnit.MINUTES)
            if (!finished) {
                proc.destroyForcibly()
                throw BadRequest("Timeout descargando torrent $baseName (60min)")
            }
            if (proc.exitValue() != 0) throw BadRequest("Error aria2c (${proc.exitValue()}) para $torrentUrl\n$output")
            val videoFiles = tempDir.walkTopDown().filter { it.isFile && it.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") }.toList()
            if (videoFiles.isEmpty()) throw BadRequest("Torrent descargado pero no se encontró video en $tempDir")
            return videoFiles.maxByOrNull { it.length() }!!
        } catch (e: BadRequest) { throw e } catch (e: Exception) { throw BadRequest("Error torrent: ${e.message}") }
    }

    private fun isCommandAvailable(cmd: String): Boolean {
        return try {
            val pb = ProcessBuilder("which", cmd).redirectErrorStream(true)
            val proc = pb.start()
            proc.waitFor(5, TimeUnit.SECONDS)
            proc.exitValue() == 0
        } catch (_: Exception) { false }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(180).ifBlank { "download.rar" }
    }

    private fun stripYear(slug: String): String {
        return slug.replace(Regex("-\\d{4}$"), "")
    }

    private fun slugifyTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .replace(Regex("-\\d{4}$"), "")
            .take(180).ifBlank { "title" }
    }

    private fun getDownloadFolderSlug(scrapeInfo: ScrapeResponse): String {
        // Si se llamó por slug, tmdbId es el slug original (ej: konosuba-gods-blessing...-2016) -> usa ese sin año
        // Si se llamó por tmdbId numérico, tmdbId es número -> usa título convertido a slug
        return if (scrapeInfo.tmdbId.contains("-") && !scrapeInfo.tmdbId.matches(Regex("^\\d+$"))) {
            stripYear(sanitizeFilename(scrapeInfo.tmdbId).removeSuffix(".rar"))
        } else {
            val title = scrapeInfo.tmdbTitle?.takeIf { it.isNotBlank() } ?: scrapeInfo.lamovieTitle
            stripYear(slugifyTitle(title).removeSuffix(".rar"))
        }
    }

    private fun normalizeType(t: String): String {
        return when (t.lowercase()) {
            "movie", "movies", "pelicula", "peliculas", "film" -> "movie"
            "tv", "series", "serie", "tvshows", "tvshow", "show", "anime", "animes" -> "tv"
            else -> throw BadRequest("type inválido: $t (use movie|tv)")
        }
    }

    private fun isSeriesType(lamovieType: String) = lamovieType.lowercase() in setOf("tvshows", "series", "tv", "animes", "anime")

    private fun isAnimeType(lamovieType: String) = lamovieType.lowercase() in setOf("animes", "anime")

    private fun isAnimeCandidate(normalizedType: String) = normalizedType == "tv"

    private fun getFastApiForType(lamovieType: String?): String {
        return if (isAnimeType(lamovieType ?: "")) lamovieLaFastApi else lamovieFastApi
    }

    private fun getBaseUrlForType(lamovieType: String?): String {
        return if (isAnimeType(lamovieType ?: "")) lamovieLaBaseUrl else lamovieBaseUrl
    }

    private fun buildLamovieUrl(slug: String, lamovieType: String): String {
        val base = getBaseUrlForType(lamovieType)
        val prefix = when {
            isAnimeType(lamovieType) -> "animes"
            isSeriesType(lamovieType) -> "series"
            else -> "peliculas"
        }
        return "$base/$prefix/$slug/"
    }

    private fun fetchTmdbInfo(tmdbId: String, type: String): Pair<String, String?> {
        val keys = buildList {
            if (tmdbApiKeyProp.isNotBlank()) add(tmdbApiKeyProp)
            addAll(fallbackTmdbKeys)
        }.distinct()
        var lastError: Exception? = null
        for (k in keys) {
            try {
                val endpoint = if (type == "movie") "movie" else "tv"
                val url = "https://api.themoviedb.org/3/$endpoint/$tmdbId?api_key=$k&language=es-MX"
                val body = httpGetString(url, mapOf("Accept" to "application/json"))
                val node = mapper.readTree(body)
                if (node.has("success") && node.get("success").asBoolean() == false) throw BadRequest(node.get("status_message")?.asText() ?: "TMDB error")
                val title = when (type) {
                    "movie" -> node.get("title")?.asText() ?: node.get("original_title")?.asText() ?: throw NoExists(tmdbId, "TMDB title")
                    else -> node.get("name")?.asText() ?: node.get("original_name")?.asText() ?: throw NoExists(tmdbId, "TMDB title")
                }
                val dateStr = when (type) {
                    "movie" -> node.get("release_date")?.asText()
                    else -> node.get("first_air_date")?.asText()
                }
                val year = dateStr?.takeIf { it.length >= 4 }?.substring(0, 4)
                return title to year
            } catch (e: Exception) {
                lastError = e
                if (e.message?.contains("401") == true || e.message?.contains("429") == true) continue
                if (e is NoExists || e is BadRequest) throw e
            }
        }
        throw NoExists(tmdbId, "TMDB info: ${lastError?.message}")
    }

    private fun searchLamovie(title: String): List<JsonNode> {
        val q = URLEncoder.encode(title, Charsets.UTF_8)
        val fastApis = listOf(lamovieFastApi, lamovieLaFastApi)
        val all = mutableListOf<JsonNode>()
        val seen = mutableSetOf<Long>()
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/search?filter=%5B%5D&postType=any&q=$q&postsPerPage=10"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: continue
                val posts = data.get("posts") ?: continue
                if (!posts.isArray) continue
                for (p in posts) {
                    val id = p.get("_id")?.asLong() ?: continue
                    if (seen.add(id)) all.add(p)
                }
                if (all.size >= 10) break
            } catch (_: Exception) {}
        }
        return all
    }

    private fun pickBestPost(posts: List<JsonNode>, tmdbTitle: String, tmdbYear: String?, type: String): JsonNode? {
        val normTitle = normalize(tmdbTitle)
        var best: JsonNode? = null
        var bestScore = -1
        for (p in posts) {
            val title = p.get("title")?.asText() ?: ""
            val orig = p.get("original_title")?.asText() ?: ""
            val slug = p.get("slug")?.asText() ?: ""
            val releaseDate = p.get("release_date")?.asText() ?: ""
            val year = releaseDate.takeIf { it.length >= 4 }?.substring(0, 4)
            var score = 0
            if (normalize(title).contains(normTitle) || normTitle.contains(normalize(title))) score += 10
            if (normalize(orig).contains(normTitle) || normTitle.contains(normalize(orig))) score += 8
            if (normalize(slug).contains(normTitle.replace(" ", "-"))) score += 5
            if (tmdbYear != null && year == tmdbYear) score += 10
            val lamovieType = p.get("type")?.asText() ?: ""
            val isSeries = isSeriesType(lamovieType)
            if ((type == "movie" && !isSeries) || (type == "tv" && isSeries)) score += 5
            if (score > bestScore) {
                bestScore = score
                best = p
            }
        }
        return best
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun findEpisode(seriesId: Long, season: Int, episode: Int): JsonNode {
        val fastApis = listOf(lamovieLaFastApi, lamovieFastApi)
        var lastError: Exception? = null
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/single/episodes/list?_id=$seriesId&season=$season&page=1&postsPerPage=50"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: throw NoExists("$seriesId", "Episodes S${season}E${episode}")
                val posts = data.get("posts") ?: throw NoExists("$seriesId", "Episodes S${season}")
                if (!posts.isArray || posts.size() == 0) continue
                for (ep in posts) {
                    val s = ep.get("season_number")?.asInt()
                    val e = ep.get("episode_number")?.asInt()
                    if (s == season && e == episode) return ep
                }
            } catch (e: NoExists) {
                lastError = e
                if (e.message?.contains("HTTP 404") == true) continue
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: NoExists("$seriesId", "Episodio S${season}E${episode}")
    }

    private fun fetchPlayer(postId: Long): JsonNode {
        val fastApis = listOf(lamovieLaFastApi, lamovieFastApi)
        var lastError: Exception? = null
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/player?postId=$postId&demo=0"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: throw NoExists("$postId", "LaMovie player")
                if (data.has("downloads") && data.get("downloads").isArray && data.get("downloads").size() > 0) return data
                if (!data.has("error") || !data.get("error").asBoolean()) return data
            } catch (e: NoExists) {
                lastError = e
                if (e.message?.contains("HTTP 404") == true) continue
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (lastError != null) throw lastError
        val url = "$lamovieFastApi/player?postId=$postId&demo=0"
        val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
        val node = mapper.readTree(body)
        return node.get("data") ?: throw NoExists("$postId", "LaMovie player")
    }

    private fun priority(n: JsonNode): Int {
        val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase()
        return when {
            u.contains("mediafire.com") -> 0
            u.contains("mega.nz") -> 1
            u.startsWith("magnet:") || u.contains(".torrent") -> 2
            else -> 3
        }
    }

    private fun httpGetString(url: String, headers: Map<String, String>): String {
        val builder = HttpRequest.newBuilder(URI(url)).GET().timeout(Duration.ofSeconds(20))
        headers.forEach { (k, v) -> builder.header(k, v) }
        val request = builder.build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            if (response.statusCode() == 404) throw NoExists(url, "HTTP 404")
            throw BadRequest("Error HTTP ${response.statusCode()} al consultar $url: ${response.body().take(300)}")
        }
        return response.body()
    }
}

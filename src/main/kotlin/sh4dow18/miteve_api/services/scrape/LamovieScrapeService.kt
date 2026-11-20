package sh4dow18.miteve_api.services.scrape

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.scrape.MediafireDownloadInfo
import sh4dow18.miteve_api.dtos.scrape.MediafireResolveResponse
import sh4dow18.miteve_api.dtos.scrape.ScrapeResponse
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.errors.ServiceUnavailable
import sh4dow18.miteve_api.services.scrape.fallback.AnimeFallbackService
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
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
        val tmdbIsAnime = tmdbInfo.third
        val (searchPosts, searchFailed) = searchLamovieWithStatus(tmdbTitle)
        // Filtro estricto por tipo: movie -> solo movies+animes, tv -> solo tvshows+animes.
        // Si el título existe pero solo en el tipo contrario, se trata como no encontrado.
        val filteredPosts = filterByType(searchPosts, normalizedType)
        if (filteredPosts.isEmpty()) {
            // Ningún resultado del tipo pedido en lamovie.
            // El fallback a sitios de anime SOLO aplica si TMDB confirma que es anime
            // (animación japonesa). Una serie/película live-action nunca debe ir a
            // jkanime/latanime: devolverían archivos ajenos en vez de 404.
            if (tmdbIsAnime) {
                val attempted = mutableListOf<String>("lamovie")
                val fallback = fallbackService?.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, tmdbTitle, "animes", season, episode, attempted)
                if (fallback != null) {
                    return fallback.copy(attemptedProviders = attempted)
                }
                if (searchFailed) throw lamovieDownError("búsqueda de '$tmdbTitle'")
            } else if (searchFailed) {
                throw lamovieDownError("búsqueda de '$tmdbTitle'")
            }
            throw NoExists(tmdbId, "LaMovie content for title $tmdbTitle")
        }
        val best = pickBestPost(filteredPosts, tmdbTitle, tmdbYear, normalizedType)
            ?: throw NoExists(tmdbId, "LaMovie content for title $tmdbTitle (sin coincidencia de título, solo ${if (normalizedType == "movie") "películas/anime" else "series/anime"})")
        // Seguridad extra: si el mejor post es del tipo contrario (no debería por el filtro), no encontrado.
        if (!isAllowedType(best.get("type").asText(), normalizedType)) {
            throw NoExists(tmdbId, "LaMovie content for title $tmdbTitle (solo ${if (normalizedType == "movie") "películas/anime" else "series/anime"})")
        }
        val lamovieId = best.get("_id").asLong()
        val lamovieSlug = best.get("slug").asText()
        val lamovieTitle = best.get("title").asText()
        val lamovieType = best.get("type").asText()
        val sourceApi = sourceApiOf(best)
        val lamovieUrl = buildLamovieUrlFromSource(lamovieSlug, lamovieType, sourceApi)
        var targetPostId = lamovieId
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        try {
            if (isSeriesType(lamovieType)) {
                seasonNum = season ?: 1
                episodeNum = episode ?: 1
                val ep = findEpisode(lamovieId, seasonNum, episodeNum, sourceApi)
                targetPostId = ep.get("_id").asLong()
                episodeId = targetPostId
            }
        } catch (e: Exception) {
            // Si lamovie no tiene la temporada/episodio y es anime, busca la temporada
            // en el fallback latino antes de fallar. Para live-action es 404 directo.
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw e
        }
        val player = try {
            fetchPlayer(targetPostId, sourceApi)
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
            // Sin descargas: si lamovie tiene las descargas pausadas, repórtalo (503)
            // en vez de "no encontrado".
            if (isLamovieDownloadsPaused()) throw lamoviePausedError()
            throw NoExists("$targetPostId", "Downloads (modal vacío) for $lamovieTitle")
        }
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText())
            u.lowercase().contains("mediafire.com")
        }
        val latinoCandidates = mediafireCandidates.filter { n -> isLatinoLang(n.get("lang")?.takeIf { !it.isNull }?.asText()) }

        // ========== ANIME: SOLO LATINO (nunca japonés) ==========
        // Si es anime, se ignora lamovie.la cuando solo hay japonés y se fuerza fallback latino.
        // Flujo: lamovie.la (se busca latino) -> JKanime -> MonosChinos -> AnimeFLV -> Consumet
        if (isAnimeType(lamovieType)) {
            if (latinoCandidates.isNotEmpty()) {
                // Hay latino en lamovie.la, úsalo directamente (no fallback)
                val mediafireNodeLatino = latinoCandidates.minByOrNull { priority(it) }!!
                val mediafireUrl = cleanUrl(if (mediafireNodeLatino.has("url_raw")) mediafireNodeLatino.get("url_raw").asText() else mediafireNodeLatino.get("url").asText())
                val resolved = try {
                    resolveLamovieMediafire(mediafireUrl)
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

        // ========== NO-ANIME (película/serie): SOLO LATINO ==========
        // Todo (películas, series y anime) debe ser en latino. Si no hay latino,
        // aunque exista en otro idioma, se responde como no encontrado.
        // El fallback anime solo aplica si TMDB confirma anime (p. ej. película anime
        // clasificada como "movies" en lamovie). Jamás para live-action.
        if (latinoCandidates.isEmpty()) {
            if (tmdbIsAnime && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fallback = fallbackService.tryFallback(tmdbTitle, tmdbYear, tmdbId, normalizedType, lamovieSlug, lamovieType, season, episode, attempted)
                if (fallback != null) return fallback.copy(attemptedProviders = attempted)
            }
            throw NoExists("$targetPostId", "No se encontró versión en español latino para '$lamovieTitle' (${mediafireCandidates.size} mediafire en otro idioma, 0 latino). Solo se permite latino.")
        }

        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: throw NoExists("$targetPostId", "Mediafire download card latino")
        val mediafireUrl = cleanUrl(if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText())
        val resolved = try {
            resolveLamovieMediafire(mediafireUrl)
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
        val singleSource = sourceApiOf(single)
        // Filtro estricto por tipo en slug: movie -> solo movies+animes, tv -> solo tvshows+animes.
        if (!isAllowedType(lamovieType, normalizedType)) {
            throw NoExists(slug, "LaMovie slug $slug no es del tipo pedido (solo ${if (normalizedType == "movie") "películas/anime" else "series/anime"})")
        }
        val lamovieUrl = buildLamovieUrlFromSource(lamovieSlug, lamovieType, singleSource)
        var targetPostId = lamovieId
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        try {
            if (isSeriesType(lamovieType)) {
                seasonNum = season ?: 1
                episodeNum = episode ?: 1
                val ep = findEpisode(lamovieId, seasonNum, episodeNum, singleSource)
                targetPostId = ep.get("_id").asLong()
                episodeId = targetPostId
            }
        } catch (e: Exception) {
            // Si lamovie no tiene la temporada/episodio y es anime, busca la temporada
            // en el fallback latino antes de fallar. Para live-action es 404 directo.
            if (isAnimeType(lamovieType) && fallbackService?.isFallbackEnabled() == true) {
                val attempted = mutableListOf<String>("lamovie")
                val fb = fallbackService.tryFallbackBySlug(slug, normalizedType, season, episode, attempted)
                if (fb != null) return fb.copy(attemptedProviders = attempted)
            }
            throw e
        }
        val player = try {
            fetchPlayer(targetPostId, singleSource)
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
            // Sin descargas: si lamovie tiene las descargas pausadas, repórtalo (503).
            if (isLamovieDownloadsPaused()) throw lamoviePausedError()
            throw NoExists("$targetPostId", "Downloads (modal vacío) for $lamovieTitle")
        }
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText())
            u.lowercase().contains("mediafire.com")
        }
        val latinoCandidates = mediafireCandidates.filter { n -> isLatinoLang(n.get("lang")?.takeIf { !it.isNull }?.asText()) }

        // ========== ANIME SLUG: SOLO LATINO (nunca japonés) ==========
        if (isAnimeType(lamovieType)) {
            if (latinoCandidates.isNotEmpty()) {
                val mediafireNodeLatino = latinoCandidates.minByOrNull { priority(it) }!!
                val mediafireUrl = cleanUrl(if (mediafireNodeLatino.has("url_raw")) mediafireNodeLatino.get("url_raw").asText() else mediafireNodeLatino.get("url").asText())
                val resolved = try {
                    resolveLamovieMediafire(mediafireUrl)
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

        // ========== NO-ANIME SLUG (película/serie): SOLO LATINO ==========
        // Todo debe ser en latino; si no hay latino, aunque exista en otro idioma, no encontrado.
        if (latinoCandidates.isEmpty()) {
            throw NoExists("$targetPostId", "No se encontró versión en español latino para slug '$slug' (${mediafireCandidates.size} mediafire en otro idioma, 0 latino). Solo se permite latino.")
        }
        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: throw NoExists("$targetPostId", "Mediafire download card latino")
        val mediafireUrl = cleanUrl(if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText())
        val resolved = try {
            resolveLamovieMediafire(mediafireUrl)
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
        // Estricto por tipo pedido (lo que pide el usuario):
        // movie -> solo movies + animes (nunca series), tv -> solo tvshows + animes (nunca movies).
        // Antes se probaban los tres y un slug de serie podía resolverse como película y viceversa.
        val normalizedForCandidates = if (postType == "movies") "movie" else "tv"
        val candidates = allowedPostTypes(normalizedForCandidates).distinct()
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
                return tagWithSource(data, fastApi)
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
                    val filtered = filterByType(searchResults, if (postType == "movies") "movie" else "tv")
                    if (filtered.isNotEmpty()) {
                        // Solo devuelve si hay coincidencia real de título; si no, prueba
                        // la siguiente query en vez de devolver una serie ajena.
                        val best = pickBestPost(filtered, baseSearch, null, if (postType == "movies") "movie" else "tv")
                        if (best != null) return best
                    }
                }
            } catch (_: Exception) {}
        }
        throw lastError ?: NoExists(slug, "LaMovie slug $postType")
    }

    companion object {
        // MediaFire está tras Cloudflare: con UA genérico ("Mozilla/5.0") responde
        // 403 + "<title>Just a moment...</title>". Con headers de navegador real
        // (verificado en vivo contra /file/7pnx3g9wdxhnjfd: MIN -> 403, FULL -> 200)
        // pasa el challenge. No volver a "Mozilla/5.0" a secas.
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        private const val BROWSER_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    }

    private fun browserHeaders(url: String, extra: Map<String, String> = emptyMap()): Map<String, String> {
        val h = mutableMapOf(
            "User-Agent" to BROWSER_UA,
            "Accept" to BROWSER_ACCEPT,
            "Accept-Language" to "es-ES,es;q=0.9,en;q=0.8",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Referer" to "https://www.google.com/"
        )
        // Para JSON (API lamovie/TMDB) Accept json tiene prioridad si se pidió.
        // Se ignora UA genérico ("Mozilla/5.0" a secas): lo bloquea Cloudflare.
        extra.forEach { (k, v) ->
            if (k == "User-Agent" && v.trim() == "Mozilla/5.0") return@forEach
            // En mediafire el Accept escueto ("text/html") también provoca 403.
            if (k == "Accept" && url.lowercase().contains("mediafire.com") && !v.contains("json")) return@forEach
            h[k] = v
        }
        if (url.lowercase().contains("mediafire.com")) {
            h["Accept"] = BROWSER_ACCEPT
            h.putIfAbsent("Sec-Fetch-Site", "none")
        }
        return h
    }

    override fun resolveMediafire(mediafireUrl: String): MediafireResolveResponse {
        val cleanedInput = cleanUrl(mediafireUrl)
        if (!cleanedInput.lowercase().contains("mediafire.com")) throw BadRequest("URL no es de mediafire: $cleanedInput")
        val body = httpGetString(cleanedInput, browserHeaders(cleanedInput, mapOf("Accept" to "text/html")))
        var direct = Regex("""aria-label="Download file"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""id="downloadButton"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""href="(https://download[^"]+mediafire[^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) direct = Regex("""href="(https://download[^"]+)"""").find(body)?.groupValues?.get(1)
        if (direct == null) throw NoExists(cleanedInput, "Mediafire direct link")
        direct = cleanUrl(direct.replace("&amp;", "&"))
        val filename = try {
            val uri = toSafeUri(direct)
            uri.path.substringAfterLast("/").takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
        return MediafireResolveResponse(mediafireUrl = cleanedInput, directUrl = direct, filename = filename)
    }

    override fun getDirectDownloadStream(mediafireUrl: String): Pair<String, java.io.InputStream> {
        val resolved = resolveMediafire(mediafireUrl)
        val directUrl = cleanUrl(resolved.directUrl)
        val dlHeaders = browserHeaders(directUrl)
        val reqBuilder = HttpRequest.newBuilder(toSafeUri(directUrl)).GET()
            .timeout(Duration.ofSeconds(60))
        dlHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
        val request = reqBuilder.build()
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
        val singleSource = sourceApiOf(single)
        if (!isAllowedType(lamovieType, normalizedType)) {
            throw NoExists(slug, "LaMovie slug $slug no es del tipo pedido (solo ${if (normalizedType == "movie") "películas/anime" else "series/anime"})")
        }
        val lamovieUrl = buildLamovieUrlFromSource(lamovieSlug, lamovieType, singleSource)
        var targetPostId = lamovieId
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        var episodeId: Long? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum, singleSource)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = fetchPlayer(targetPostId, singleSource)
        val downloadsNode = player.get("downloads") ?: throw NoExists("$targetPostId", "Downloads torrent fallback slug")
        val downloadsList = downloadsNode.toList()
        val torrentCandidatesSlug = downloadsList.filter { n -> (n.get("quality")?.takeIf { !it.isNull }?.asText()?.lowercase() ?: "").contains("1080p") }
            .filter { n -> val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase(); u.startsWith("magnet:") || u.contains(".torrent") }
        val latinoTorrentCandidatesSlug = torrentCandidatesSlug.filter { n -> isLatinoLang(n.get("lang")?.takeIf { !it.isNull }?.asText()) }
        // Películas, series y anime: SOLO latino, nunca otro idioma.
        if (latinoTorrentCandidatesSlug.isEmpty()) {
            throw NoExists("$targetPostId", "No se encontró torrent latino 1080p para '$lamovieTitle' (solo otro idioma). Solo latino permitido.")
        }
        val poolTorrentSlug = latinoTorrentCandidatesSlug
        val torrentNode = poolTorrentSlug.minByOrNull { n -> val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase(); if (u.contains(".torrent")) 0 else 1 }
            ?: throw NoExists("$targetPostId", "Torrent latino 1080p not found slug")
        val torrentUrl = cleanUrl(if (torrentNode.has("url_raw")) torrentNode.get("url_raw").asText() else torrentNode.get("url").asText())
        val scrapeInfo = ScrapeResponse(slug, normalizedType, lamovieTitle, null, lamovieId, lamovieSlug, lamovieTitle, lamovieUrl, lamovieType, seasonNum, episodeNum, episodeId, MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", "latino", null), null)
        return downloadTorrentToServer(torrentUrl, scrapeInfo)
    }

    fun downloadTorrentFallback(tmdbId: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        return downloadTorrentFallbackInternal(tmdbId, type, season, episode)
    }

    internal fun downloadTorrentFallbackInternal(tmdbId: String, type: String, season: Int?, episode: Int?): sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse {
        val normalizedType = normalizeType(type)
        val tmdbInfo = fetchTmdbInfo(tmdbId, normalizedType)
        val (searchPosts, searchFailed) = searchLamovieWithStatus(tmdbInfo.first)
        val filteredSearch = filterByType(searchPosts, normalizedType)
        if (filteredSearch.isEmpty()) {
            if (searchFailed) throw lamovieDownError("torrent fallback de '$tmdbId'")
            throw NoExists(tmdbId, "LaMovie content for torrent fallback")
        }
        val best = pickBestPost(filteredSearch, tmdbInfo.first, tmdbInfo.second, normalizedType)
            ?: throw NoExists(tmdbId, "LaMovie content for torrent fallback (sin coincidencia de título)")
        if (!isAllowedType(best.get("type").asText(), normalizedType)) {
            throw NoExists(tmdbId, "LaMovie content for torrent fallback (tipo contrario)")
        }
        val lamovieId = best.get("_id").asLong()
        val lamovieSlug = best.get("slug").asText()
        val lamovieTitle = best.get("title").asText()
        val lamovieType = best.get("type").asText()
        val bestSource = sourceApiOf(best)
        val lamovieUrl = buildLamovieUrlFromSource(lamovieSlug, lamovieType, bestSource)
        var targetPostId = lamovieId
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        var episodeId: Long? = null
        if (isSeriesType(lamovieType)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisode(lamovieId, seasonNum, episodeNum, bestSource)
            targetPostId = ep.get("_id").asLong()
            episodeId = targetPostId
        }
        val player = fetchPlayer(targetPostId, bestSource)
        val downloadsNode = player.get("downloads") ?: throw NoExists("$targetPostId", "Downloads torrent fallback")
        val downloadsList = downloadsNode.toList()
        val torrentCandidates = downloadsList.filter { n ->
            val q = n.get("quality")?.takeIf { !it.isNull }?.asText()?.lowercase() ?: ""
            q.contains("1080p")
        }.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase()
            u.startsWith("magnet:") || u.contains(".torrent") || u.contains("magnet")
        }
        val latinoTorrentCandidates = torrentCandidates.filter { n -> isLatinoLang(n.get("lang")?.takeIf { !it.isNull }?.asText()) }
        // Películas, series y anime: SOLO latino.
        if (latinoTorrentCandidates.isEmpty()) {
            throw NoExists("$targetPostId", "No se encontró torrent latino 1080p para '$lamovieTitle' (solo otro idioma). Solo latino permitido.")
        }
        val poolTorrent = latinoTorrentCandidates
        val torrentNode = poolTorrent.minByOrNull { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url").asText()).lowercase()
            if (u.contains(".torrent")) 0 else 1
        } ?: throw NoExists("$targetPostId", "Torrent latino 1080p not found")
        val torrentUrl = cleanUrl(if (torrentNode.has("url_raw")) torrentNode.get("url_raw").asText() else torrentNode.get("url").asText())
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
            mediafire = MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", "latino", null),
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
        // Limpia la URL: el error "Illegal character in path at index 43" viene de espacios/saltos
        // invisibles al final. También detecta si nos pasaron la página intermedia de
        // mediafire (/file/...) en vez del directo.
        var safeDirectUrl = cleanUrl(directUrl)
        val safeOriginal = originalMediafireUrl?.let { cleanUrl(it) }
        // Si el directo sigue siendo página intermedia (/file/) intenta resolver de nuevo ahora.
        if (isMediafireSharePage(safeDirectUrl)) {
            val toResolve = safeOriginal?.takeIf { it.lowercase().contains("mediafire.com") } ?: safeDirectUrl
            try {
                val re = resolveMediafire(toResolve)
                safeDirectUrl = cleanUrl(re.directUrl)
                if (isMediafireSharePage(safeDirectUrl)) {
                    throw BadRequest("El enlace de mediafire es página intermedia y no se pudo obtener el directo (source=${scrapeInfo?.source}). Intenta de nuevo o reporta. URL: $safeDirectUrl")
                }
            } catch (e: BadRequest) {
                throw e
            } catch (e: Exception) {
                throw BadRequest("No se pudo resolver el directo de mediafire para $safeDirectUrl (source=${scrapeInfo?.source}): ${e.message}")
            }
        }
        // Valida URI antes de descargar para dar error claro en vez de Illegal character críptico.
        try {
            toSafeUri(safeDirectUrl)
        } catch (e: Exception) {
            throw BadRequest("URL de descarga inválida '$safeDirectUrl' (source=${scrapeInfo?.source}): ${e.message}")
        }
        val baseDir = Paths.get(downloadDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
        // Para fallbacks latinos (latanime, jkanime, etc.) el mediafire ya es .mp4 directo, no .rar
        val isMp4Direct = safeDirectUrl.lowercase().contains(".mp4")
        val isMkvDirect = safeDirectUrl.lowercase().contains(".mkv")
        val isStream = safeDirectUrl.contains(".m3u8") || scrapeInfo?.source in setOf("consumet", "animeflv", "latanime", "jkanime", "monoschinos", "tioanime", "animefenix", "nyaa") && (isMp4Direct || isMkvDirect)
        val extension = when {
            safeDirectUrl.contains(".m3u8") -> ".m3u8"
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
            val request = HttpRequest.newBuilder(toSafeUri(safeDirectUrl)).GET()
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
                println("[Download] HttpClient HTTP $lastStatus para $safeDirectUrl, intenta curl fallback")
                throw BadRequest("HTTP $lastStatus")
            }
        } catch (e: Exception) {
            lastError = e
            if (e.message?.contains("HTTP 404") == true || e.message?.contains("HTTP 403") == true || lastStatus in setOf(404,403,429,520)) {
                // Fallback via curl -L
                try {
                    println("[Download] Fallback curl para $safeDirectUrl (scrape source=${scrapeInfo?.source})")
                    val curlCmd = arrayOf("curl", "-s", "-L", "-A", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", "-o", normalizedTarget.toString(), safeDirectUrl)
                    val pb = ProcessBuilder(*curlCmd).redirectErrorStream(true)
                    val proc = pb.start()
                    val finished = proc.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)
                    if (!finished) { proc.destroyForcibly(); throw BadRequest("Timeout curl para $safeDirectUrl") }
                    if (proc.exitValue() != 0) {
                        val out = proc.inputStream.bufferedReader().readText().take(500)
                        throw BadRequest("curl fallo ${proc.exitValue()} para $safeDirectUrl: $out")
                    }
                    if (!Files.exists(normalizedTarget) || Files.size(normalizedTarget) == 0L) {
                        throw BadRequest("curl descargó vacío para $safeDirectUrl")
                    }
                    lastStatus = 200
                    lastError = null
                } catch (curlE: Exception) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $safeDirectUrl (curl fallback falló: ${curlE.message}, orig: ${e.message}, source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl})")
                }
            } else if (lastStatus == -1) {
                try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                throw BadRequest("Error al descargar en servidor: ${e.message} para $safeDirectUrl (source=${scrapeInfo?.source})")
            } else {
                try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $safeDirectUrl (source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl})")
            }
        }
        if (lastStatus !in 200..299 && lastError == null) {
            // Si curl tuvo éxito, lastStatus ya es 200
        } else if (lastStatus !in 200..299) {
            throw BadRequest("Error al descargar en servidor: HTTP $lastStatus para $safeDirectUrl")
        }
        val fileSize = try { Files.size(normalizedTarget) } catch (_: Exception) { null }
        // Verifica que no sea HTML (37.5kB indica página HTML, no video)
        try {
            if (fileSize != null && fileSize < 5 * 1024 * 1024) {
                val head = Files.readAllBytes(normalizedTarget).take(2048).toByteArray().toString(Charsets.UTF_8).lowercase()
                if (head.contains("<html") || head.contains("<!doctype") || head.contains("<head")) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Descarga resultó en HTML (${fileSize} bytes) en vez de video para $safeDirectUrl (source=${scrapeInfo?.source}, mediafire=${scrapeInfo?.mediafire?.mediafireUrl}, episode S${scrapeInfo?.season}E${scrapeInfo?.episode}). Posible mediafire directo expirado o latanime bloqueado. Intenta de nuevo o reporta.")
                }
                // También si es muy pequeño y no es video esperado (episodio debe ser >50MB)
                if (fileSize < 50 * 1024 && head.contains("mediafire") ) {
                    try { Files.deleteIfExists(normalizedTarget) } catch (_: Exception) {}
                    throw BadRequest("Archivo muy pequeño (${fileSize} bytes) parece página mediafire, no video. DirectUrl: $safeDirectUrl")
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
            mediafireUrl = scrapeInfo?.mediafire?.mediafireUrl ?: safeOriginal ?: "",
            directUrl = safeDirectUrl,
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

    // Tanto película como serie pueden ser anime (película anime / serie anime),
    // por eso el fallback latino aplica a ambos tipos.
    private fun isAnimeCandidate(normalizedType: String) = normalizedType == "tv" || normalizedType == "movie"

    // Filtro estricto por tipo pedido:
    // movie -> solo movies + animes (nunca series/tvshows)
    // tv    -> solo tvshows + animes (nunca movies)
    private fun isAllowedType(lamovieType: String, normalizedType: String): Boolean {
        val lt = lamovieType.lowercase()
        return when (normalizedType) {
            "movie" -> lt in setOf("movies", "movie", "pelicula", "peliculas", "animes", "anime")
            else -> lt in setOf("tvshows", "tvshow", "tv", "series", "serie", "animes", "anime")
        }
    }

    private fun filterByType(posts: List<JsonNode>, normalizedType: String): List<JsonNode> {
        return posts.filter { p ->
            val t = p.get("type")?.takeIf { !it.isNull }?.asText() ?: return@filter false
            isAllowedType(t, normalizedType)
        }
    }

    // Solo latino estricto: películas, series y anime deben ser latino.
    // Acepta "latino/latin/latín/doblado/doblaje". Rechaza castellano, subtitulado,
    // japonés, español solo, inglés, etc. Si no es latino -> como no encontrado.
    private fun isLatinoLang(lang: String?): Boolean {
        val l = lang?.lowercase()?.trim() ?: return false
        if (l.isBlank()) return false
        return l.contains("latino") || l.contains("latin") || l.contains("latín") || l.contains("dobl")
    }

    // Limpia URLs de mediafire/descarga: quita espacios, saltos y caracteres invisibles
    // que provocan "Illegal character in path at index 43" en URI().
    fun cleanUrl(raw: String): String {
        var u = raw.trim().replace(Regex("[\\r\\n\\t\\u0000-\\u001F\\u007F]"), "").trim()
        u = u.trim('"', '\'', '<', '>', '(', ')', '[', ']')
        while (u.endsWith(".") || u.endsWith(",") || u.endsWith(";") || u.endsWith("!") || u.endsWith(")")) {
            u = u.dropLast(1).trim()
        }
        u = u.replace(" ", "%20")
        return u.trim()
    }

    private fun toSafeUri(url: String): java.net.URI {
        val cleaned = cleanUrl(url)
        if (cleaned.isBlank()) throw BadRequest("URL vacía")
        try {
            return java.net.URI(cleaned)
        } catch (e: Exception) {
            throw BadRequest("URL inválida '$cleaned': ${e.message}")
        }
    }

    // Página intermedia de mediafire (/file/...) vs directo (download*.mediafire.com).
    private fun isMediafireSharePage(url: String): Boolean {
        val l = url.lowercase()
        if (!l.contains("mediafire.com")) return false
        if (l.contains("download") && l.contains("mediafire")) return false
        return l.contains("/file/") || l.contains("/folder/") || l.trimEnd('/').endsWith("mediafire.com")
    }

    // ========== Estado de lamovie.org (caído / descargas pausadas) ==========
    @Volatile private var pauseCacheAt: Long = 0L
    @Volatile private var pauseCacheValue: Boolean = false
    private val pauseCacheTtlMs = 90_000L

    private fun isLamovieDownloadsPaused(): Boolean {
        val now = System.currentTimeMillis()
        if (now - pauseCacheAt < pauseCacheTtlMs) return pauseCacheValue
        val paused = checkPauseBanner()
        pauseCacheAt = now
        pauseCacheValue = paused
        return paused
    }

    private fun checkPauseBanner(): Boolean {
        val markers = listOf(
            "descargas del sitio están pausadas",
            "descargas pausadas",
            "problema puntual en el servidor",
            "pico de tráfico",
            "incidencia en el centro de datos"
        )
        for (base in listOf(lamovieBaseUrl, lamovieLaBaseUrl).distinct()) {
            try {
                val body = httpGetString(base, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html")).lowercase()
                if (markers.any { body.contains(it) }) {
                    println("[Lamovie] Banner de descargas pausadas detectado en $base")
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    // ¿El fallo parece caída de lamovie (5xx/red/Cloudflare) y no un 404 legítimo?
    private fun isDownLike(e: Exception?): Boolean {
        if (e == null) return false
        val m = (e.message ?: "").lowercase()
        if (m.contains("http 404")) return false
        return m.contains("http 5") || m.contains("http 403") || m.contains("http 429") ||
            m.contains("http 520") || m.contains("http 521") || m.contains("http 522") ||
            m.contains("http 523") || m.contains("http 524") ||
            m.contains("timeout") || m.contains("timed out") || m.contains("connection") ||
            m.contains("connect") || m.contains("unreachable") || m.contains("cloudflare") ||
            m.contains("no route to host") || m.contains("unknownhost") || m.contains("reset")
    }

    private fun lamovieDownError(detail: String?): ServiceUnavailable {
        val msg = buildString {
            append("lamovie.org no responde")
            if (!detail.isNullOrBlank()) append(" (fallo al consultar su API: ${detail.take(200)})")
            append(". Intenta más tarde.")
        }
        println("[Lamovie] API caída: $msg")
        return ServiceUnavailable(msg)
    }

    private fun lamoviePausedError(): ServiceUnavailable {
        val msg = "lamovie.org tiene las descargas pausadas temporalmente: reportan un problema " +
            "puntual en su servidor (posible pico de tráfico o incidencia en el centro de datos). Intenta más tarde."
        println("[Lamovie] Descargas pausadas: $msg")
        return ServiceUnavailable(msg)
    }

    // Resuelve un mediafire de lamovie informando la pausa de descargas si aplica.
    private fun resolveLamovieMediafire(mediafireUrl: String): MediafireResolveResponse {
        try {
            return resolveMediafire(mediafireUrl)
        } catch (e: Exception) {
            if (isLamovieDownloadsPaused()) throw lamoviePausedError()
            throw e
        }
    }

    // lamovie.org y lamovie.la tienen espacios de IDs DISTINTOS (ej: Reacher S1E7 es
    // 38457 en org pero 11523 en la; el 38457 en la es la película "Cambio de turno").
    // Por eso, una vez elegido el post, episodios y player deben consultarse en la MISMA
    // fuente donde se encontró, no a ciegas en la otra (eso devolvía una película como
    // si fuera el capítulo, o "0 mediafire"). Cada post lleva su fuente en _sourceApi.
    private fun tagWithSource(node: JsonNode, api: String): JsonNode {
        return try {
            if (node.isObject) {
                (node as tools.jackson.databind.node.ObjectNode).put("_sourceApi", api)
            }
            node
        } catch (_: Exception) { node }
    }

    private fun sourceApiOf(node: JsonNode): String? {
        return node.get("_sourceApi")?.takeIf { !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
    }

    // Tipos permitidos por búsqueda (lo que pide el usuario):
    // movie -> solo movies + animes (nunca series/tvshows)
    // tv    -> solo tvshows + animes (nunca movies)
    private fun allowedPostTypes(normalizedType: String): List<String> {
        return if (normalizedType == "movie") listOf("movies", "animes")
        else listOf("tvshows", "animes")
    }

    private fun baseUrlForApi(api: String?): String {
        if (api != null && api.contains("lamovie.la")) return lamovieLaBaseUrl
        return lamovieBaseUrl
    }

    private fun buildLamovieUrl(slug: String, lamovieType: String): String {
        return buildLamovieUrlFromSource(slug, lamovieType, null)
    }

    private fun buildLamovieUrlFromSource(slug: String, lamovieType: String, sourceApi: String?): String {
        val base = baseUrlForApi(sourceApi)
        val prefix = when {
            isAnimeType(lamovieType) -> "animes"
            isSeriesType(lamovieType) -> "series"
            else -> "peliculas"
        }
        // lamovie.la usa /series/ y /peliculas/ igual que org para series/pelis.
        return "$base/$prefix/$slug/"
    }

    private fun fetchTmdbInfo(tmdbId: String, type: String): Triple<String, String?, Boolean> {
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
                // Puerta anime: el fallback a sitios de anime solo aplica si TMDB dice
                // que es animación japonesa. Así una serie live-action jamás termina
                // en un proveedor de anime con archivos ajenos.
                val genreIds = mutableSetOf<Int>()
                node.get("genres")?.takeIf { it.isArray }?.forEach { g ->
                    g.get("id")?.asInt()?.let { genreIds.add(it) }
                }
                node.get("genre_ids")?.takeIf { it.isArray }?.forEach { g ->
                    genreIds.add(g.asInt())
                }
                val origLang = node.get("original_language")?.asText()?.lowercase() ?: ""
                val originCountries = mutableSetOf<String>()
                node.get("origin_country")?.takeIf { it.isArray }?.forEach { c ->
                    c.asText()?.let { originCountries.add(it.uppercase()) }
                }
                val isAnime = genreIds.contains(16) && (origLang == "ja" || originCountries.contains("JP"))
                return Triple(title, year, isAnime)
            } catch (e: Exception) {
                lastError = e
                if (e.message?.contains("401") == true || e.message?.contains("429") == true) continue
                if (e is NoExists || e is BadRequest) throw e
            }
        }
        throw NoExists(tmdbId, "TMDB info: ${lastError?.message}")
    }

    // Búsqueda en wp-api/v1 (probada en vivo: lamovie.org y lamovie.la responden).
    // Devuelve (posts, allFailed) para distinguir "no encontrado" de "lamovie caído".
    // Cada post lleva _sourceApi (org o la) porque los IDs no son intercambiables.
    private fun searchLamovieWithStatus(title: String): Pair<List<JsonNode>, Boolean> {
        val q = URLEncoder.encode(title, Charsets.UTF_8)
        val fastApis = listOf(lamovieFastApi, lamovieLaFastApi)
        val all = mutableListOf<JsonNode>()
        val seen = mutableSetOf<String>()
        var attempts = 0
        var downFailures = 0
        for (fastApi in fastApis) {
            attempts++
            try {
                val url = "$fastApi/search?filter=%5B%5D&postType=any&q=$q&postsPerPage=10"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: continue
                val posts = data.get("posts") ?: continue
                if (!posts.isArray) continue
                for (p in posts) {
                    val id = p.get("_id")?.asLong() ?: continue
                    // Clave por fuente: el mismo número en org y la es CONTENIDO DISTINTO.
                    val key = "$fastApi:$id"
                    if (seen.add(key)) all.add(tagWithSource(p, fastApi))
                }
                if (all.size >= 10) break
            } catch (e: NoExists) {
                if (e.message?.contains("HTTP 404") != true) downFailures++
            } catch (e: Exception) {
                if (isDownLike(e)) downFailures++ else downFailures++
            }
        }
        return all to (all.isEmpty() && attempts > 0 && downFailures >= attempts)
    }

    private fun searchLamovie(title: String): List<JsonNode> {
        return searchLamovieWithStatus(title).first
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
            // Señal de título: exige que el título coincida de verdad. Sin esto, un
            // post ajeno sumaría puntos y se descargarían archivos que nada que ver.
            var titleScore = 0
            if (normalize(title).contains(normTitle) || normTitle.contains(normalize(title))) titleScore += 10
            if (normalize(orig).contains(normTitle) || normTitle.contains(normalize(orig))) titleScore += 8
            if (normalize(slug).contains(normTitle.replace(" ", "-"))) titleScore += 5
            if (titleScore == 0) continue
            var score = titleScore
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

    private fun findEpisode(seriesId: Long, season: Int, episode: Int, sourceApi: String? = null): JsonNode {
        // Los IDs no son intercambiables entre org y la: se consulta SOLO la fuente
        // donde se encontró la serie. Consultar la otra devuelve otra serie (vacío)
        // y nunca el episodio pedido.
        val fastApis = if (!sourceApi.isNullOrBlank()) listOf(sourceApi) else listOf(lamovieFastApi, lamovieLaFastApi)
        var lastError: Exception? = null
        var foundEmpty = false
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/single/episodes/list?_id=$seriesId&season=$season&page=1&postsPerPage=50"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: throw NoExists("$seriesId", "Episodes S${season}E${episode}")
                val posts = data.get("posts") ?: throw NoExists("$seriesId", "Episodes S${season}")
                if (!posts.isArray || posts.size() == 0) {
                    foundEmpty = true
                    continue
                }
                for (ep in posts) {
                    val s = ep.get("season_number")?.asInt()
                    val e = ep.get("episode_number")?.asInt()
                    if (s == season && e == episode) return tagWithSource(ep, fastApi)
                }
                // Hay episodios pero no el pedido: 404 directo, sin probar otra fuente.
                throw NoExists("$seriesId", "Episodio S${season}E${episode}")
            } catch (e: NoExists) {
                lastError = e
                if (e.message?.contains("HTTP 404") == true) continue
                // "Episodio no existe": no probar otra fuente (sería otro contenido).
                if (e.message?.contains("Episodio S") == true) throw e
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (isDownLike(lastError)) throw lamovieDownError("episodios de $seriesId S${season}E${episode}")
        if (foundEmpty && lastError == null) throw NoExists("$seriesId", "Episodio S${season}E${episode}")
        throw lastError ?: NoExists("$seriesId", "Episodio S${season}E${episode}")
    }

    private fun fetchPlayer(postId: Long, sourceApi: String? = null): JsonNode {
        // Igual que episodios: el player se consulta SOLO en la fuente del post.
        // El mismo número en la otra fuente es OTRO contenido (ej: 38457 = Reacher
        // S1E7 en org y película "Cambio de turno" en la). Aceptar el de la otra
        // fuente descargaba una película como si fuera el capítulo.
        val fastApis = if (!sourceApi.isNullOrBlank()) listOf(sourceApi) else listOf(lamovieFastApi, lamovieLaFastApi)
        var lastError: Exception? = null
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/player?postId=$postId&demo=0"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: throw NoExists("$postId", "LaMovie player")
                if (data.has("downloads") && data.get("downloads").isArray && data.get("downloads").size() > 0) return data
                if (!data.has("error") || !data.get("error").asBoolean()) return data
                // error=true con downloads vacío en la fuente propia: modal vacío real.
                return data
            } catch (e: NoExists) {
                lastError = e
                if (e.message?.contains("HTTP 404") == true) continue
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (isDownLike(lastError)) throw lamovieDownError("reproductor de $postId")
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
        val safeUri = try {
            toSafeUri(url)
        } catch (e: BadRequest) {
            throw e
        } catch (e: Exception) {
            throw BadRequest("URL inválida '$url': ${e.message}")
        }
        // Enriquece a headers de navegador real: Cloudflare (MediaFire) bloquea
        // el "Mozilla/5.0" genérico con 403 + "Just a moment...".
        val enriched = browserHeaders(url, headers).toMutableMap()
        // Si piden JSON explícitamente, respétalo (APIs lamovie/TMDB).
        headers["Accept"]?.takeIf { it.contains("json") }?.let { enriched["Accept"] = it }
        try {
            val builder = HttpRequest.newBuilder(safeUri).GET().timeout(Duration.ofSeconds(20))
            enriched.forEach { (k, v) -> builder.header(k, v) }
            val request = builder.build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                // Cloudflare a veces devuelve 200 con challenge embebido; en ese
                // caso cae al fallback curl igual que con 403.
                if (!isCloudflareChallenge(response.body())) return response.body()
                println("[Scrape] HttpClient recibió challenge Cloudflare para $url, intenta curl fallback")
            } else if (response.statusCode() == 404) {
                throw NoExists(url, "HTTP 404")
            } else if (response.statusCode() !in setOf(403, 429, 503, 520, 521, 522, 523, 524)) {
                throw BadRequest("Error HTTP ${response.statusCode()} al consultar $url: ${response.body().take(300)}")
            } else {
                println("[Scrape] HttpClient HTTP ${response.statusCode()} para $url, intenta curl fallback")
            }
        } catch (e: NoExists) {
            throw e
        } catch (e: BadRequest) {
            // Error ya formateado (p.ej. 4xx no reintentable): propágalo, salvo que
            // parezca bloqueo Cloudflare/bot, que sí merece el fallback curl.
            if (!isDownLike(e)) throw e
            println("[Scrape] HttpClient error $url: ${e.message}, intenta curl fallback")
        } catch (e: Exception) {
            println("[Scrape] HttpClient error $url: ${e.message}, intenta curl fallback")
        }
        // Fallback via curl: su fingerprint TLS/HTTP2 pasa Cloudflare donde
        // Java HttpClient es marcado como bot (verificado: MIN -> 403, FULL -> 200).
        return try {
            val cmd = mutableListOf(
                "curl", "-s", "-L", "--http2", "--compressed",
                "-A", enriched["User-Agent"]!!,
                "-H", "Accept: ${enriched["Accept"]}",
                "-H", "Accept-Language: ${enriched["Accept-Language"]}",
                "-H", "Upgrade-Insecure-Requests: 1",
                "-H", "Sec-Fetch-Dest: document",
                "-H", "Sec-Fetch-Mode: navigate",
                "-H", "Sec-Fetch-Site: none",
                "-H", "Referer: ${enriched["Referer"]}",
                "--max-time", "25",
                cleanUrl(url)
            )
            val pb = ProcessBuilder(cmd).redirectErrorStream(true)
            pb.environment()["LC_ALL"] = "C"
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                throw BadRequest("Timeout curl para $url")
            }
            if (proc.exitValue() != 0) throw BadRequest("curl falló ${proc.exitValue()} para $url: ${output.take(200)}")
            if (output.isBlank()) throw BadRequest("curl vacío para $url")
            if (isCloudflareChallenge(output)) {
                throw BadRequest(
                    "MediaFire bloqueó la petición con challenge Cloudflare (HTTP 403 'Just a moment...') para $url. " +
                        "Ni HttpClient ni curl con headers de navegador lo superaron desde esta IP. Reintenta más tarde o desde otra red."
                )
            }
            output
        } catch (e: BadRequest) {
            throw e
        } catch (e: Exception) {
            throw BadRequest("Error HTTP al consultar $url (HttpClient+curl fallaron): ${e.message}")
        }
    }

    private fun isCloudflareChallenge(body: String): Boolean {
        return body.contains("<title>Just a moment") ||
            body.contains("Checking if the site connection is secure") ||
            (body.contains("cf-mitigated") && body.length < 20_000 && body.lowercase().contains("cloudflare"))
    }
}

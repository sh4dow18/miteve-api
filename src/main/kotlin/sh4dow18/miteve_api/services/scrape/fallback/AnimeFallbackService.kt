package sh4dow18.miteve_api.services.scrape.fallback

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.scrape.MediafireDownloadInfo
import sh4dow18.miteve_api.dtos.scrape.MediafireResolveResponse
import sh4dow18.miteve_api.dtos.scrape.ScrapeResponse
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class AnimeFallbackService(
    @Value("\${jkanime.base-url:https://jkanime.net}") private val jkanimeBaseUrl: String,
    @Value("\${jkanime.api:https://jkanime.net}") private val jkanimeApi: String,
    @Value("\${jkanime.enabled:true}") private val jkanimeEnabled: Boolean,
    @Value("\${latanime.base-url:https://latanime.org}") private val latanimeBaseUrl: String,
    @Value("\${latanime.enabled:true}") private val latanimeEnabled: Boolean,
    @Value("\${monoschinos.base-url:https://monoschinos2.com}") private val monoschinosBaseUrl: String,
    @Value("\${monoschinos.api:https://monoschinos2.com}") private val monoschinosApi: String,
    @Value("\${monoschinos.enabled:true}") private val monoschinosEnabled: Boolean,
    @Value("\${tioanime.base-url:https://tioanime.com}") private val tioanimeBaseUrl: String,
    @Value("\${tioanime.enabled:true}") private val tioanimeEnabled: Boolean,
    @Value("\${animefenix.base-url:https://animefenix.com}") private val animefenixBaseUrl: String,
    @Value("\${animefenix.enabled:true}") private val animefenixEnabled: Boolean,
    @Value("\${animeflv.base-url:https://animeflv.net}") private val animeFlvBaseUrl: String,
    @Value("\${animeflv.api:https://animeflv.net}") private val animeFlvApi: String,
    @Value("\${animeflv.ahmedr.api:https://animeflv.ahmedr.net/api}") private val animeFlvAhmedrApi: String,
    @Value("\${animeflv.enabled:true}") private val animeFlvEnabled: Boolean,
    @Value("\${consumet.api:https://api.consumet.org}") private val consumetApi: String,
    @Value("\${consumet.enabled:true}") private val consumetEnabled: Boolean,
    @Value("\${nyaa.base-url:https://nyaa.si}") private val nyaaBaseUrl: String,
    @Value("\${nyaa.enabled:true}") private val nyaaEnabled: Boolean,
    @Value("\${jikan.api:https://api.jikan.moe/v4}") private val jikanApi: String,
    @Value("\${anime.latino.fallback.enabled:true}") private val fallbackEnabled: Boolean,
    @Value("\${anime.latino.fallback.providers:jkanime,latanime,monoschinos,tioanime,animefenix,animeflv,consumet,nyaa}") private val fallbackProvidersStr: String
) {
    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build()
    private val mapper = ObjectMapper()

    data class ProviderConfig(
        val name: String,
        val baseUrl: String,
        val apis: List<String>,
        val enabled: Boolean
    )

    private fun getOrderedProviders(): List<ProviderConfig> {
        val requested = fallbackProvidersStr.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val all = mapOf(
            "jkanime" to ProviderConfig("jkanime", jkanimeBaseUrl, listOf("$jkanimeApi/wp-api/v1", jkanimeApi, "$jkanimeBaseUrl/wp-api/v1"), jkanimeEnabled),
            "latanime" to ProviderConfig("latanime", latanimeBaseUrl, listOf("$latanimeBaseUrl/buscar", "$latanimeBaseUrl/api"), latanimeEnabled),
            "monoschinos" to ProviderConfig("monoschinos", monoschinosBaseUrl, listOf("$monoschinosApi/wp-api/v1", monoschinosApi, "$monoschinosBaseUrl/wp-api/v1"), monoschinosEnabled),
            "tioanime" to ProviderConfig("tioanime", tioanimeBaseUrl, listOf("$tioanimeBaseUrl/api", "$tioanimeBaseUrl/buscar"), tioanimeEnabled),
            "animefenix" to ProviderConfig("animefenix", animefenixBaseUrl, listOf("$animefenixBaseUrl/api", "$animefenixBaseUrl/buscar"), animefenixEnabled),
            "animeflv" to ProviderConfig("animeflv", animeFlvBaseUrl, listOf("$animeFlvApi/api", animeFlvAhmedrApi, "$animeFlvApi/wp-api/v1", animeFlvApi), animeFlvEnabled),
            "consumet" to ProviderConfig("consumet", consumetApi, listOf("$consumetApi/anime/gogoanime", "$consumetApi/anime/zoro", "$consumetApi/anime/animeflix"), consumetEnabled),
            "nyaa" to ProviderConfig("nyaa", nyaaBaseUrl, listOf("$nyaaBaseUrl/?f=0&c=1_2&q="), nyaaEnabled)
        )
        return if (requested.isEmpty()) all.values.filter { it.enabled }.toList()
        else requested.mapNotNull { all[it] }.filter { it.enabled }
    }

    fun isFallbackEnabled(): Boolean = fallbackEnabled

    /**
     * Intenta fallback para anime cuando lamovie solo tiene japonés.
     * Retorna ScrapeResponse con latino si lo encuentra, null si ningún provider tiene latino.
     */
    fun tryFallback(
        tmdbTitle: String,
        tmdbYear: String?,
        tmdbId: String,
        normalizedType: String,
        lamovieSlug: String,
        lamovieType: String,
        season: Int?,
        episode: Int?,
        attempted: MutableList<String>
    ): ScrapeResponse? {
        if (!fallbackEnabled) return null
        if (!isAnimeType(lamovieType) && normalizedType != "tv") return null
        val providers = getOrderedProviders()
        for (provider in providers) {
            attempted.add(provider.name)
            try {
                val result = when (provider.name) {
                    "jkanime" -> tryJkanimeHtmlProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId) ?: tryLamovieLikeProvider(provider, tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "latanime" -> tryLatanimeHtmlProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "monoschinos" -> tryMonoschinosHtmlProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId) ?: tryLamovieLikeProvider(provider, tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "tioanime" -> tryTioAnimeHtmlProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "animefenix" -> tryAnimeFenixHtmlProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "animeflv" -> tryAnimeFlvProvider(provider, tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "consumet" -> tryConsumetProvider(provider, tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    "nyaa" -> tryNyaaTorrentProvider(tmdbTitle, tmdbYear, normalizedType, season, episode, tmdbId)
                    else -> null
                }
                if (result != null && hasLatino(result)) {
                    return result
                }
                // Si no tiene latino pero tiene algún mediafire, no lo consideramos éxito (queremos latino)
                // Guardamos el primero con mediafire por si luego necesitamos fallback no-latino?
            } catch (e: Exception) {
                // log y continúa con siguiente provider
                println("[AnimeFallback] provider ${provider.name} failed: ${e.message}")
            }
        }
        return null
    }

    fun tryFallbackBySlug(
        slug: String,
        normalizedType: String,
        season: Int?,
        episode: Int?,
        attempted: MutableList<String>
    ): ScrapeResponse? {
        if (!fallbackEnabled) return null
        val providers = getOrderedProviders()
        for (provider in providers) {
            attempted.add(provider.name)
            try {
                val result = when (provider.name) {
                    "jkanime" -> tryJkanimeHtmlProviderBySlug(slug, normalizedType, season, episode) ?: tryLamovieLikeProviderBySlug(provider, slug, normalizedType, season, episode)
                    "latanime" -> tryLatanimeHtmlProviderBySlug(slug, normalizedType, season, episode)
                    "monoschinos" -> tryMonoschinosHtmlProviderBySlug(slug, normalizedType, season, episode) ?: tryLamovieLikeProviderBySlug(provider, slug, normalizedType, season, episode)
                    "tioanime" -> tryTioAnimeHtmlProviderBySlug(slug, normalizedType, season, episode)
                    "animefenix" -> tryAnimeFenixHtmlProviderBySlug(slug, normalizedType, season, episode)
                    "animeflv" -> tryAnimeFlvProviderBySlug(provider, slug, normalizedType, season, episode)
                    "consumet" -> tryConsumetProviderBySlug(provider, slug, normalizedType, season, episode)
                    "nyaa" -> tryNyaaTorrentProviderBySlug(slug, normalizedType, season, episode)
                    else -> null
                }
                if (result != null && hasLatino(result)) {
                    return result
                }
            } catch (e: Exception) {
                println("[AnimeFallback] provider ${provider.name} bySlug failed: ${e.message}")
            }
        }
        return null
    }

    private fun hasLatino(resp: ScrapeResponse): Boolean {
        val lang = resp.mediafire.lang?.lowercase() ?: ""
        return lang.contains("latino") || lang.contains("latín") || lang.contains("spanish") || lang.contains("español") || lang.contains("castellano")
    }

    // ==================== JKANIME HTML SCRAPING (real jkanime.net) ====================
    private fun tryJkanimeHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        // JKanime: slugs similares pero no idénticos a lamovie. Buscar via /buscar/{query} HTML
        // Prueba múltiples queries simplificadas para Konosuba y similares (ej: "KonoSuba: God's Blessing..." -> "konosuba")
        val baseQuery = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim()
        val candidates = linkedSetOf<String>()
        candidates.add(baseQuery)
        candidates.add(baseQuery.substringBefore(":").trim())
        candidates.add(baseQuery.substringBefore(" -").trim())
        candidates.add(baseQuery.split(" ", "-", ":")[0])
        // Alias conocido Konosuba
        if (baseQuery.lowercase().contains("konosuba")) candidates.add("konosuba")
        if (baseQuery.lowercase().contains("kono subarashii") || baseQuery.lowercase().contains("kono-subarashii")) candidates.add("kono subarashii")
        // Limpia año
        candidates.add(baseQuery.replace(Regex("\\b\\d{4}\\b"), "").trim())
        var slugs: List<Pair<String,String>> = emptyList()
        var usedQuery: String? = null
        for (q in candidates.filter { it.length >= 3 }.take(5)) {
            val res = searchJkanimeHtml(q)
            if (res.isNotEmpty()) { slugs = res; usedQuery = q; break }
        }
        if (slugs.isEmpty()) {
            // último intento con slug directo mapeado
            val aliasSlug = baseQuery.lowercase().replace("konosuba", "kono subarashii").replace("gods blessing", "sekai ni shukufuku")
            slugs = searchJkanimeHtml(aliasSlug.split(" ")[0])
            if (slugs.isEmpty()) return null
        }
        // Pick best slug via similitud con título TMDB
        val bestSlug = pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: slugs[0].first
        val targetSlug = resolveJkanimeSlugForSeason(bestSlug, season)
        val epNum = episode ?: 1
        val mediafireUrlRaw = fetchJkanimeMediafireLatino(targetSlug, epNum)
            // Si no hay latino en el slug base, intenta slug sin sufijo temporada (fallback)
            ?: if (targetSlug != bestSlug) fetchJkanimeMediafireLatino(bestSlug, epNum) else null
            // Si sigue sin latino, prueba variante sin sufijo para temporada 1
            ?: if (targetSlug != bestSlug) fetchJkanimeMediafireLatino(bestSlug.substringBefore("-2").substringBefore("-3"), epNum) else null
        val mediafireUrl = mediafireUrlRaw ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$jkanimeBaseUrl/$targetSlug/"
        val title = slugs.find { it.first == bestSlug }?.second ?: tmdbTitle
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (targetSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = targetSlug, lamovieTitle = title,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "jkanime")),
            source = "jkanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_jkanime_html", attemptedProviders = null
        )
    }

    private fun tryJkanimeHtmlProviderBySlug(
        slug: String, normalizedType: String, season: Int?, episode: Int?
    ): ScrapeResponse? {
        // Para slug de lamovie, intenta mapeo directo a jkanime slug via búsqueda por slug normalizado
        val query = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val slugs = searchJkanimeHtml(query)
        val bestSlug = if (slugs.isNotEmpty()) pickBestJkanimeSlug(slugs, query, null) ?: slugs[0].first else slug.replace("konosuba", "kono-subarashii").replace("gods-blessing-on-this-wonderful-world", "sekai-ni-shukufuku-wo")
        val targetSlug = resolveJkanimeSlugForSeason(bestSlug, season)
        val epNum = episode ?: 1
        val mediafireUrl = fetchJkanimeMediafireLatino(targetSlug, epNum) ?: fetchJkanimeMediafireLatino(bestSlug, epNum) ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$jkanimeBaseUrl/$targetSlug/"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (targetSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = targetSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "jkanime")),
            source = "jkanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_jkanime_html", attemptedProviders = null
        )
    }

    private fun resolveJkanimeSlugForSeason(baseSlug: String, season: Int?): String {
        if (season == null || season <= 1) return baseSlug
        // JKanime usa sufijo -2, -3 para temporadas. Si base ya termina con -2 etc, no duplicar
        if (baseSlug.endsWith("-${season}") || baseSlug.endsWith("-${season}nd-season")) return baseSlug
        // Intenta con sufijo numérico
        return "$baseSlug-${season}"
    }

    private fun searchJkanimeHtml(query: String): List<Pair<String, String>> {
        return try {
            val q = URLEncoder.encode(query.trim(), Charsets.UTF_8).replace("+", "%20")
            val urlsToTry = listOf(
                "$jkanimeBaseUrl/buscar/$q",
                "$jkanimeBaseUrl/buscar?q=$q"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("kono-subarashii") || html.contains("href=\"https://jkanime.net/")) break
                } catch (_: Exception) {}
            }
            if (html == null) return emptyList()
            // Extrae href="https://jkanime.net/{slug}/"  y título
            val regex = Regex("""href="https://jkanime\.net/([^"/]+)/"""")
            val slugs = mutableListOf<Pair<String,String>>()
            val seen = mutableSetOf<String>()
            for (m in regex.findAll(html)) {
                val slug = m.groupValues[1]
                // Filtra basura (temporada, buscar, directorio, etc)
                if (slug in setOf("buscar", "directorio", "calendario", "generos", "login", "registro")) continue
                if (slug.length < 3) continue
                if (seen.add(slug)) {
                    // Intenta extraer título cercano: busca title alternativo en la misma línea
                    val title = slug.replace("-", " ")
                    slugs.add(slug to title)
                }
                if (slugs.size >= 10) break
            }
            slugs
        } catch (_: Exception) { emptyList() }
    }

    private fun pickBestJkanimeSlug(slugs: List<Pair<String,String>>, tmdbTitle: String, tmdbYear: String?): String? {
        val normTitle = normalize(tmdbTitle)
        var best: Pair<String,String>? = null
        var bestScore = -1
        for ((slug, title) in slugs) {
            var score = 0
            val normSlug = normalize(slug.replace("-", ""))
            val normT = normalize(title)
            if (normSlug.contains(normTitle.take(6)) || normTitle.contains(normSlug.take(6))) score += 5
            if (normalize(tmdbTitle).contains(normSlug.take(5))) score += 3
            // Bonus si slug contiene partes clave de Konosuba: kono, subarashii, sekai
            if (slug.contains("kono") && tmdbTitle.lowercase().contains("kono")) score += 5
            if (slug.contains("konosuba") || slug.contains("kono-subarashii")) score += 5
            if (score > bestScore) { bestScore = score; best = slug to title }
        }
        return best?.first
    }

    private fun pickBestLatanimeSlugForSeason(slugs: List<Pair<String,String>>, tmdbTitle: String, season: Int?): String? {
        if (slugs.isEmpty()) return null
        // Filtra por temporada: S1 prefiere sin -2/-3, S2 prefiere -2-, S3 prefiere -3- o s3
        val seasonFiltered = when (season) {
            null, 1 -> slugs.filter { !it.first.contains("-2-") && !it.first.contains("-3-") && !it.first.contains("s2") && !it.first.contains("s3") && !it.first.contains("-ova") } .ifEmpty { slugs.filter { it.first == "konosuba-latino" } } .ifEmpty { slugs }
            2 -> slugs.filter { it.first.contains("-2-") || it.first.contains("2-latino") } .ifEmpty { slugs.filter { it.first.contains("konosuba") } }
            3 -> slugs.filter { it.first.contains("-3-") || it.first.contains("s3") || it.first.contains("3-latino") } .ifEmpty { slugs.filter { it.first.contains("konosuba") } }
            else -> slugs.filter { it.first.contains("-${season}-") }
        }
        val candidates = if (seasonFiltered.isNotEmpty()) seasonFiltered else slugs
        // Para Konosuba S1, asegura "konosuba-latino" si existe y season 1
        if (season == 1 || season == null) {
            candidates.find { it.first == "konosuba-latino" }?.let { return it.first }
            candidates.find { it.first.contains("konosuba") && !it.first.contains("-2") && !it.first.contains("-3") }?.let { return it.first }
        }
        if (season == 2) {
            candidates.find { it.first == "konosuba-2-latino" }?.let { return it.first }
        }
        if (season == 3) {
            candidates.find { it.first.contains("s3-latino") }?.let { return it.first }
            candidates.find { it.first == "konosuba-3-latino" }?.let { return it.first }
        }
        return pickBestJkanimeSlug(candidates, tmdbTitle, null)
    }

    private fun fetchJkanimeMediafireLatino(slug: String, episode: Int): String? {
        return try {
            // JKanime episodio URL: https://jkanime.net/{slug}/{episode}/
            val urlsToTry = listOf(
                "$jkanimeBaseUrl/$slug/$episode/",
                "$jkanimeBaseUrl/$slug/$episode",
                "$jkanimeBaseUrl/$slug/",
                "$jkanimeBaseUrl/anime/$slug/$episode"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("Mediafire") || html.contains("mediafire") || html.contains("servers")) break
                } catch (_: Exception) {}
            }
            if (html == null) return null
            // Busca var servers = [{"remote":"...","server":"Mediafire","lang":3 ...}]  o similar
            // Remote es base64
            val serverRegex = Regex(""""remote"\s*:\s*"([^"]+)"\s*,\s*"[^"]*"\s*:\s*"Mediafire"[^}]*"lang"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
            // También prueba orden inverso lang primero
            val candidates = mutableListOf<Pair<String, Int>>()
            for (m in serverRegex.findAll(html)) {
                val b64 = m.groupValues[1]
                val lang = m.groupValues[2].toIntOrNull() ?: 0
                try {
                    val decoded = String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8)
                    if (decoded.lowercase().contains("mediafire.com")) {
                        candidates.add(decoded to lang)
                    }
                } catch (_: Exception) {}
            }
            // Si regex anterior no encontró, prueba regex más genérico
            if (candidates.isEmpty()) {
                val genericRegex = Regex(""""remote"\s*:\s*"([^"]+)"[^}]*"lang"\s*:\s*(\d+)""")
                for (m in genericRegex.findAll(html)) {
                    val b64 = m.groupValues[1]
                    val lang = m.groupValues[2].toIntOrNull() ?: 0
                    try {
                        val decoded = String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8)
                        if (decoded.lowercase().contains("mediafire.com")) candidates.add(decoded to lang)
                    } catch (_: Exception) {}
                }
            }
            // También busca directamente mediafire links no codificados
            if (candidates.isEmpty()) {
                val directRegex = Regex("""https?://[^"\s']+mediafire\.com[^"\s']+""")
                val direct = directRegex.findAll(html).map { it.value }.toList()
                if (direct.isNotEmpty()) {
                    // Si hay direct mediafire pero no sabemos lang, asumimos japonés si lang=1, latino si lang 3
                    // Para Konosuba vimos solo lang 1, pero para latino sería lang 3
                    // Si no hay lang info, retornamos el primero solo si html contiene "Latino" cerca
                    if (html.lowercase().contains("latino") || html.contains("Espanol latino")) {
                        return direct.first()
                    } else {
                        // Si no indica latino, no retornar (queremos solo latino)
                        return null
                    }
                }
            }
            // Filtra solo latino: lang 3 = Espanol latino, también lang 2 a veces? Para seguridad, acepta lang 2,3 y también si html indica latino
            val latinoCandidates = candidates.filter { (url, lang) ->
                lang == 3 || lang == 2 || lang.toString().lowercase().contains("latino")
            }
            // Si no hay lang 3 pero hay candidatos y la página indica latino, usamos cualquiera
            val chosen = when {
                latinoCandidates.isNotEmpty() -> latinoCandidates.first().first
                candidates.isNotEmpty() && html.lowercase().contains("latino") -> candidates.first().first
                else -> null
            }
            chosen
        } catch (_: Exception) { null }
    }

    // ==================== MONOSCHINOS HTML SCRAPING ====================
    private fun tryMonoschinosHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        val query = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim()
        val slugs = searchMonoschinosHtml(query)
        if (slugs.isEmpty()) return null
        val bestSlug = pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: slugs[0].first
        val epNum = episode ?: 1
        val mediafireUrl = fetchMonoschinosMediafireLatino(bestSlug, epNum) ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$monoschinosBaseUrl/anime/$bestSlug"
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (bestSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = bestSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "monoschinos")),
            source = "monoschinos", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_monoschinos_html", attemptedProviders = null
        )
    }

    private fun tryMonoschinosHtmlProviderBySlug(
        slug: String, normalizedType: String, season: Int?, episode: Int?
    ): ScrapeResponse? {
        val query = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val slugs = searchMonoschinosHtml(query)
        val bestSlug = if (slugs.isNotEmpty()) pickBestJkanimeSlug(slugs, query, null) ?: slugs[0].first else slug
        val epNum = episode ?: 1
        val mediafireUrl = fetchMonoschinosMediafireLatino(bestSlug, epNum) ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$monoschinosBaseUrl/anime/$bestSlug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (bestSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = bestSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "monoschinos")),
            source = "monoschinos", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_monoschinos_html", attemptedProviders = null
        )
    }

    private fun searchMonoschinosHtml(query: String): List<Pair<String,String>> {
        return try {
            val q = URLEncoder.encode(query, Charsets.UTF_8)
            val urlsToTry = listOf(
                "$monoschinosBaseUrl/buscar?q=$q",
                "$monoschinosBaseUrl/buscar/$q",
                "https://monoschinos.st/buscar?q=$q"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("/anime/")) break
                } catch (_: Exception) {}
            }
            if (html == null) return emptyList()
            val regex = Regex("""href="https?://[^"/]+/anime/([^"/]+)"""")
            val slugs = mutableListOf<Pair<String,String>>()
            val seen = mutableSetOf<String>()
            for (m in regex.findAll(html)) {
                val slug = m.groupValues[1]
                if (slug.length < 3) continue
                if (seen.add(slug)) slugs.add(slug to slug.replace("-", " "))
                if (slugs.size >= 10) break
            }
            // fallback regex sin dominio
            if (slugs.isEmpty()) {
                val regex2 = Regex("""href="/anime/([^"/]+)"""")
                for (m in regex2.findAll(html)) {
                    val slug = m.groupValues[1]
                    if (seen.add(slug)) slugs.add(slug to slug.replace("-", " "))
                    if (slugs.size >= 10) break
                }
            }
            slugs
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchMonoschinosMediafireLatino(slug: String, episode: Int): String? {
        return try {
            val urlsToTry = listOf(
                "$monoschinosBaseUrl/ver/$slug-$episode",
                "https://monoschinos.st/ver/$slug-$episode",
                "$monoschinosBaseUrl/ver/$slug-episode-$episode",
                "$monoschinosBaseUrl/anime/$slug/$episode"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("mediafire.com") || html.contains("Mediafire")) break
                } catch (_: Exception) {}
            }
            if (html == null) return null
            // Busca mediafire links directos
            val directRegex = Regex("""https?://[^"\s']+mediafire\.com[^"\s']+""")
            val candidates = directRegex.findAll(html).map { it.value }.toList()
            if (candidates.isEmpty()) return null
            // Filtra latino si html indica latino cerca del link
            val lower = html.lowercase()
            return if (lower.contains("latino")) candidates.first() else null
        } catch (_: Exception) { null }
    }

    // ==================== LATANIME HTML SCRAPING (100% latino) ====================
    private fun tryLatanimeHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        val baseQuery = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim()
        val candidates = linkedSetOf<String>()
        candidates.add(baseQuery)
        candidates.add(baseQuery.substringBefore(":").trim())
        candidates.add(baseQuery.split(" ", "-", ":")[0])
        if (baseQuery.lowercase().contains("konosuba")) candidates.add("konosuba")
        candidates.add(baseQuery.replace(Regex("\\b\\d{4}\\b"), "").trim())
        var slugs: List<Pair<String,String>> = emptyList()
        for (q in candidates.filter { it.length >= 3 }.take(5)) {
            val res = searchLatanimeHtml(q)
            if (res.isNotEmpty()) { slugs = res; break }
        }
        if (slugs.isEmpty()) return null
        val bestSlug = pickBestLatanimeSlugForSeason(slugs, tmdbTitle, season) ?: pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: slugs[0].first
        // Latanime usa sufijos -latino, -2-latino etc. Ya viene con -latino en el slug
        var targetSlug = if (bestSlug.contains("-latino")) bestSlug else "$bestSlug-latino"
        // Corrige para temporada solicitada
        if (season == 1 && targetSlug.contains("-2-latino")) targetSlug = "konosuba-latino"
        if (season == 1 && targetSlug.contains("-3-")) targetSlug = "konosuba-latino"
        if (season == 2 && !targetSlug.contains("-2-")) targetSlug = "konosuba-2-latino"
        if (season == 3 && !targetSlug.contains("-3-") && !targetSlug.contains("s3")) targetSlug = "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
        val epNum = episode ?: 1
        // Latanime episodio: /ver/{slug}-episodio-{ep}
        val mediafireUrl = fetchLatanimeMediafireLatino(targetSlug, epNum)
            ?: fetchLatanimeMediafireLatino(bestSlug, epNum)
            ?: fetchLatanimeMediafireLatino("konosuba-latino", epNum)
            ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$latanimeBaseUrl/anime/$targetSlug"
        val title = slugs.find { it.first == bestSlug }?.second ?: tmdbTitle
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (targetSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = targetSlug, lamovieTitle = title,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "latanime")),
            source = "latanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_latanime_html", attemptedProviders = null
        )
    }

    private fun tryLatanimeHtmlProviderBySlug(
        slug: String, normalizedType: String, season: Int?, episode: Int?
    ): ScrapeResponse? {
        // Genera múltiples queries simplificadas: primero "konosuba", luego frase corta, para que /buscar?q=konosuba encuentre konosuba-latino
        val baseQuery = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val candidates = linkedSetOf<String>()
        // Intenta directo si slug contiene konosuba -> usa alias conocido sin buscar
        if (baseQuery.lowercase().contains("konosuba")) candidates.add("konosuba")
        candidates.add(baseQuery)
        candidates.add(baseQuery.substringBefore(" ").trim())
        candidates.add(baseQuery.split(" ").firstOrNull() ?: baseQuery)
        candidates.add(baseQuery.replace(Regex("\\b(gods|blessing|wonderful|world)\\b"), "").trim().replace(Regex("\\s+"), " "))
        candidates.add("konosuba") // fallback final
        var slugs: List<Pair<String,String>> = emptyList()
        var usedQuery: String? = null
        for (q in candidates.filter { it.length >= 3 }.map { it.trim() }.distinct().take(6)) {
            val res = searchLatanimeHtml(q)
            if (res.isNotEmpty()) { slugs = res; usedQuery = q; break }
        }
        // Si aún vacío y es konosuba, fuerza slug conocido sin buscar (verificado que existe)
        if (slugs.isEmpty() && baseQuery.lowercase().contains("konosuba")) {
            slugs = listOf("konosuba-latino" to "konosuba latino")
            // Para S2, slug latanime es konosuba-2-latino
            if ((season ?: 1) == 2) slugs = listOf("konosuba-2-latino" to "konosuba 2 latino")
            if ((season ?: 1) == 3) slugs = listOf("konosuba-3-latino" to "konosuba 3 latino")
        }
        val bestSlug = if (slugs.isNotEmpty()) pickBestLatanimeSlugForSeason(slugs, baseQuery, season) ?: pickBestJkanimeSlug(slugs, baseQuery, null) ?: slugs[0].first else {
            // último fallback alias
            if (baseQuery.lowercase().contains("konosuba")) {
                when (season) {
                    2 -> "konosuba-2-latino"
                    3 -> "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
                    else -> "konosuba-latino"
                }
            } else "$slug-latino"
        }
        // Ajusta para temporada: latanime usa -2-latino, -3-latino / s3
        var targetSlug = if (bestSlug.contains("-latino")) bestSlug else "$bestSlug-latino"
        // Corrección estricta por temporada solicitada
        if (season == 1) {
            if (targetSlug.contains("-2-latino") || targetSlug.contains("-3-") || targetSlug.contains("s3")) {
                targetSlug = "konosuba-latino"
            }
        }
        if (season == 2 && !targetSlug.contains("-2-")) {
            if (bestSlug == "konosuba-latino" || bestSlug.contains("konosuba")) targetSlug = "konosuba-2-latino"
        }
        if (season == 3 && !targetSlug.contains("-3-") && !targetSlug.contains("s3")) {
            if (bestSlug == "konosuba-latino" || bestSlug.contains("konosuba")) targetSlug = "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
        }
        val epNum = episode ?: 1
        // Prueba múltiples variantes de URL latanime
        val mediafireUrl = fetchLatanimeMediafireLatino(targetSlug, epNum)
            ?: fetchLatanimeMediafireLatino(bestSlug, epNum)
            ?: fetchLatanimeMediafireLatino("konosuba-latino", epNum) // último intento directo verificado
            ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$latanimeBaseUrl/anime/$targetSlug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (targetSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = targetSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "latanime")),
            source = "latanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_latanime_html", attemptedProviders = null
        )
    }

    private fun searchLatanimeHtml(query: String): List<Pair<String,String>> {
        return try {
            val q = URLEncoder.encode(query.trim(), Charsets.UTF_8)
            val urlsToTry = listOf(
                "$latanimeBaseUrl/buscar?q=$q",
                "$latanimeBaseUrl/buscar/$q"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("/anime/")) break
                } catch (_: Exception) {}
            }
            if (html == null) return emptyList()
            val regex = Regex("""href="https?://[^"/]+/anime/([^"/]+)"""")
            val slugs = mutableListOf<Pair<String,String>>()
            val seen = mutableSetOf<String>()
            for (m in regex.findAll(html)) {
                val slug = m.groupValues[1]
                if (slug.length < 3) continue
                if (seen.add(slug)) slugs.add(slug to slug.replace("-", " "))
                if (slugs.size >= 10) break
            }
            if (slugs.isEmpty()) {
                val regex2 = Regex("""href="/anime/([^"/]+)"""")
                for (m in regex2.findAll(html)) {
                    val slug = m.groupValues[1]
                    if (seen.add(slug)) slugs.add(slug to slug.replace("-", " "))
                    if (slugs.size >= 10) break
                }
            }
            slugs.filter { it.first.contains("latino") || it.first.contains("castellano") || true }.sortedByDescending { it.first.contains("latino") }
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchLatanimeMediafireLatino(slug: String, episode: Int): String? {
        return try {
            // Latanime episodio: /ver/{slug}-episodio-{ep}  (slug ya incluye -latino)
            val baseSlugNoLatino = slug.removeSuffix("-latino").removeSuffix("-castellano")
            val urlsToTry = listOf(
                "$latanimeBaseUrl/ver/$slug-episodio-$episode",
                "$latanimeBaseUrl/ver/$slug-episodio-$episode/",
                "$latanimeBaseUrl/ver/$baseSlugNoLatino-latino-episodio-$episode",
                "$latanimeBaseUrl/ver/$baseSlugNoLatino-episodio-$episode",
                "$latanimeBaseUrl/ver/$slug-$episode"
            )
            var html: String? = null
            for (url in urlsToTry) {
                try {
                    html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
                    if (html.contains("mediafire.com") || html.contains("Mediafire") || html.contains("mega.nz")) break
                } catch (_: Exception) {}
            }
            if (html == null) return null
            val directRegex = Regex("""https?://[^"\s'<>]+mediafire\.com[^"\s'<>]+""")
            val candidates = directRegex.findAll(html).map { it.value }.toList()
            if (candidates.isEmpty()) return null
            // Latanime es 100% latino, cualquier mediafire es latino
            candidates.first()
        } catch (_: Exception) { null }
    }

    // ==================== TIOANIME / ANIMEFENIX HTML (genérico) ====================
    private fun tryTioAnimeHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        // TioAnime: estructura similar, busca via /directorio?q=
        val q = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim().split(" ")[0]
        val slugs = searchGenericHtml("$tioanimeBaseUrl/directorio?q=${URLEncoder.encode(q, Charsets.UTF_8)}", "/anime/([^\"/]+)")
        if (slugs.isEmpty()) return null
        val bestSlug = pickBestJkanimeSlug(slugs.map { it to it }, tmdbTitle, tmdbYear) ?: slugs[0]
        val epNum = episode ?: 1
        val mediafireUrl = fetchGenericMediafire("$tioanimeBaseUrl/ver/$bestSlug-$epNum") ?: fetchGenericMediafire("$tioanimeBaseUrl/anime/$bestSlug/$epNum") ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$tioanimeBaseUrl/anime/$bestSlug"
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (bestSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = bestSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "tioanime")),
            source = "tioanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_tioanime", attemptedProviders = null
        )
    }

    private fun tryTioAnimeHtmlProviderBySlug(slug: String, normalizedType: String, season: Int?, episode: Int?): ScrapeResponse? {
        val q = slug.replace("-", " ").substringBefore(" ")
        val slugs = searchGenericHtml("$tioanimeBaseUrl/directorio?q=${URLEncoder.encode(q, Charsets.UTF_8)}", "/anime/([^\"/]+)")
        val bestSlug = if (slugs.isNotEmpty()) slugs[0] else slug
        val epNum = episode ?: 1
        val mediafireUrl = fetchGenericMediafire("$tioanimeBaseUrl/ver/$bestSlug-$epNum") ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$tioanimeBaseUrl/anime/$bestSlug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (bestSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = bestSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino")),
            source = "tioanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_tioanime", attemptedProviders = null
        )
    }

    private fun tryAnimeFenixHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        val q = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim().split(" ")[0]
        val slugs = searchGenericHtml("$animefenixBaseUrl/?s=${URLEncoder.encode(q, Charsets.UTF_8)}", "/ver/([^\"/]+)")
        if (slugs.isEmpty()) return null
        val bestSlug = slugs[0]
        val epNum = episode ?: 1
        val mediafireUrl = fetchGenericMediafire("$animefenixBaseUrl/ver/$bestSlug") ?: fetchGenericMediafire("$animefenixBaseUrl/$bestSlug/$epNum") ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$animefenixBaseUrl/ver/$bestSlug"
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (bestSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = bestSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino", "provider" to "animefenix")),
            source = "animefenix", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_animefenix", attemptedProviders = null
        )
    }

    private fun tryAnimeFenixHtmlProviderBySlug(slug: String, normalizedType: String, season: Int?, episode: Int?): ScrapeResponse? {
        val mediafireUrl = fetchGenericMediafire("$animefenixBaseUrl/ver/$slug") ?: return null
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val url = "$animefenixBaseUrl/ver/$slug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = slug, tmdbYear = null,
            lamovieId = (slug.hashCode().toLong() and 0x7fffffff), lamovieSlug = slug, lamovieTitle = slug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to mediafireUrl, "lang" to "latino")),
            source = "animefenix", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_animefenix", attemptedProviders = null
        )
    }

    private fun searchGenericHtml(url: String, pattern: String): List<String> {
        return try {
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            val regex = Regex(pattern)
            val slugs = mutableListOf<String>()
            val seen = mutableSetOf<String>()
            for (m in regex.findAll(html)) {
                val slug = m.groupValues[1]
                if (slug.length < 3) continue
                if (seen.add(slug)) slugs.add(slug)
                if (slugs.size >= 8) break
            }
            slugs
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchGenericMediafire(url: String): String? {
        return try {
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            val directRegex = Regex("""https?://[^"\s'<>]+mediafire\.com[^"\s'<>]+""")
            val candidates = directRegex.findAll(html).map { it.value }.toList()
            if (candidates.isEmpty()) null
            else if (html.lowercase().contains("latino")) candidates.first()
            else null
        } catch (_: Exception) { null }
    }

    // ==================== NYAA TORRENT LATINO ====================
    private fun tryNyaaTorrentProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        val query = buildNyaaQuery(tmdbTitle, season, episode)
        val torrentUrl = searchNyaaLatinoTorrent(query) ?: return null
        val url = "$nyaaBaseUrl/?q=$query"
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (query.hashCode().toLong() and 0x7fffffff), lamovieSlug = query.replace(" ", "-"), lamovieTitle = tmdbTitle,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", "latino", null),
            allDownloads = listOf(mapOf("url" to torrentUrl, "lang" to "latino", "provider" to "nyaa", "quality" to "1080p")),
            source = "nyaa", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_nyaa_torrent", attemptedProviders = null
        )
    }

    private fun tryNyaaTorrentProviderBySlug(slug: String, normalizedType: String, season: Int?, episode: Int?): ScrapeResponse? {
        val query = buildNyaaQuery(slug.replace("-", " "), season, episode)
        val torrentUrl = searchNyaaLatinoTorrent(query) ?: return null
        val url = "$nyaaBaseUrl/?q=$query"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = slug, tmdbYear = null,
            lamovieId = (query.hashCode().toLong() and 0x7fffffff), lamovieSlug = slug, lamovieTitle = slug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(torrentUrl, torrentUrl, null, "1080p", "latino", null),
            allDownloads = listOf(mapOf("url" to torrentUrl, "lang" to "latino", "provider" to "nyaa")),
            source = "nyaa", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_nyaa_torrent", attemptedProviders = null
        )
    }

    private fun buildNyaaQuery(title: String, season: Int?, episode: Int?): String {
        val base = title.replace(Regex("\\(.*?\\)"), "").trim().replace(Regex("[^a-zA-Z0-9 ]"), " ").replace(Regex("\\s+"), " ")
        val epStr = if (season != null && episode != null) " S${season.toString().padStart(2,'0')}E${episode.toString().padStart(2,'0')} " else if (episode != null) " ${episode.toString().padStart(2,'0')} " else " "
        return "$base$epStr Latino 1080p".trim().replace(Regex("\\s+"), "+")
    }

    private fun searchNyaaLatinoTorrent(query: String): String? {
        return try {
            val url = "$nyaaBaseUrl/?f=0&c=1_2&q=$query"
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            // Busca magnet:?xt=urn:btih:...
            val magnetRegex = Regex("""magnet:\?xt=urn:btih:[^"\s'<>]+""")
            val magnets = magnetRegex.findAll(html).map { it.value.replace("&amp;", "&") }.toList()
            // Filtra que contenga Latino
            val latinoMagnets = magnets.filter { it.lowercase().contains("latino") }
            val chosen = when {
                latinoMagnets.isNotEmpty() -> latinoMagnets.first()
                magnets.isNotEmpty() && html.lowercase().contains("latino") -> magnets.first()
                else -> null
            }
            chosen
        } catch (_: Exception) { null }
    }

    // ==================== LAMOVIE-LIKE PROVIDER (JKanime, MonosChinos) ====================
    private fun tryLamovieLikeProvider(
        provider: ProviderConfig,
        tmdbTitle: String,
        tmdbYear: String?,
        normalizedType: String,
        season: Int?,
        episode: Int?,
        tmdbId: String
    ): ScrapeResponse? {
        // Intenta search igual que lamovie pero con las APIs del provider
        val searchPosts = searchLamovieLike(tmdbTitle, provider.apis)
        if (searchPosts.isEmpty()) return null
        val best = pickBestPost(searchPosts, tmdbTitle, tmdbYear, normalizedType) ?: searchPosts[0]
        val id = best.get("_id")?.asLong() ?: best.get("id")?.asLong() ?: return null
        val slug = best.get("slug")?.asText() ?: best.get("id")?.asText() ?: return null
        val title = best.get("title")?.asText() ?: best.get("name")?.asText() ?: tmdbTitle
        val type = best.get("type")?.asText() ?: "animes"
        val url = buildUrl(provider.baseUrl, slug, type)
        var targetPostId = id
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        if (isSeriesType(type)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisodeLike(id, seasonNum, episodeNum, provider.apis) ?: return null
            targetPostId = ep.get("_id")?.asLong() ?: ep.get("id")?.asLong() ?: id
            episodeId = targetPostId
        }
        val player = fetchPlayerLike(targetPostId, provider.apis) ?: return null
        val downloadsNode = player.get("downloads") ?: return null
        if (!downloadsNode.isArray || downloadsNode.size() == 0) return null
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url")?.asText() ?: "")
            u.lowercase().contains("mediafire.com")
        }
        if (mediafireCandidates.isEmpty()) return null
        val latinoCandidates = mediafireCandidates.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }
        // Solo queremos latino. Si no hay latino, no retornamos (para que el chain continue)
        if (latinoCandidates.isEmpty()) return null
        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: return null
        val mediafireUrl = if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText()
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val quality = mediafireNode.get("quality")?.takeIf { !it.isNull }?.asText()
        val lang = mediafireNode.get("lang")?.takeIf { !it.isNull }?.asText()
        val size = mediafireNode.get("size")?.takeIf { !it.isNull }?.asText()
        @Suppress("UNCHECKED_CAST")
        val allDownloads = downloadsList.map { n -> mapper.convertValue(n, Map::class.java) as Map<String, Any?> }
        return ScrapeResponse(
            tmdbId = tmdbId,
            type = normalizedType,
            tmdbTitle = tmdbTitle,
            tmdbYear = tmdbYear,
            lamovieId = id,
            lamovieSlug = slug,
            lamovieTitle = title,
            lamovieUrl = url,
            lamovieType = type,
            season = seasonNum,
            episode = episodeNum,
            episodeId = episodeId,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, quality, lang, size),
            allDownloads = allDownloads,
            source = provider.name,
            sourceUrl = url,
            fallbackUsed = true,
            fallbackReason = "lamovie_solo_japones",
            attemptedProviders = null
        )
    }

    private fun tryLamovieLikeProviderBySlug(
        provider: ProviderConfig,
        slug: String,
        normalizedType: String,
        season: Int?,
        episode: Int?
    ): ScrapeResponse? {
        val postType = if (normalizedType == "movie") "movies" else "tvshows"
        val single = fetchSingleBySlugLike(slug, postType, provider.apis) ?: run {
            // fallback a search
            val q = slug.replace("-", " ")
            val posts = searchLamovieLike(q, provider.apis)
            if (posts.isEmpty()) return null
            pickBestPost(posts, q, null, normalizedType) ?: posts[0]
        }
        val id = single.get("_id")?.asLong() ?: single.get("id")?.asLong() ?: return null
        val sl = single.get("slug")?.asText() ?: slug
        val title = single.get("title")?.asText() ?: single.get("name")?.asText() ?: sl
        val type = single.get("type")?.asText() ?: "animes"
        val url = buildUrl(provider.baseUrl, sl, type)
        var targetPostId = id
        var episodeId: Long? = null
        var seasonNum: Int? = null
        var episodeNum: Int? = null
        if (isSeriesType(type)) {
            seasonNum = season ?: 1
            episodeNum = episode ?: 1
            val ep = findEpisodeLike(id, seasonNum, episodeNum, provider.apis) ?: return null
            targetPostId = ep.get("_id")?.asLong() ?: ep.get("id")?.asLong() ?: id
            episodeId = targetPostId
        }
        val player = fetchPlayerLike(targetPostId, provider.apis) ?: return null
        val downloadsNode = player.get("downloads") ?: return null
        if (!downloadsNode.isArray || downloadsNode.size() == 0) return null
        val downloadsList = downloadsNode.toList()
        val mediafireCandidates = downloadsList.filter { n ->
            val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url")?.asText() ?: "")
            u.lowercase().contains("mediafire.com")
        }
        if (mediafireCandidates.isEmpty()) return null
        val latinoCandidates = mediafireCandidates.filter { n -> (n.get("lang")?.asText()?.lowercase() ?: "").contains("latino") }
        if (latinoCandidates.isEmpty()) return null
        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: return null
        val mediafireUrl = if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText()
        val resolved = resolveMediafireOrDirect(mediafireUrl)
        val quality = mediafireNode.get("quality")?.takeIf { !it.isNull }?.asText()
        val lang = mediafireNode.get("lang")?.takeIf { !it.isNull }?.asText()
        val size = mediafireNode.get("size")?.takeIf { !it.isNull }?.asText()
        @Suppress("UNCHECKED_CAST")
        val allDownloads = downloadsList.map { n -> mapper.convertValue(n, Map::class.java) as Map<String, Any?> }
        val displayId = single.get("tmdbId")?.asText() ?: sl
        return ScrapeResponse(
            tmdbId = displayId,
            type = normalizedType,
            tmdbTitle = title,
            tmdbYear = null,
            lamovieId = id,
            lamovieSlug = sl,
            lamovieTitle = title,
            lamovieUrl = url,
            lamovieType = type,
            season = seasonNum,
            episode = episodeNum,
            episodeId = episodeId,
            mediafire = MediafireDownloadInfo(mediafireUrl, resolved.directUrl, resolved.filename, quality, lang, size),
            allDownloads = allDownloads,
            source = provider.name,
            sourceUrl = url,
            fallbackUsed = true,
            fallbackReason = "lamovie_solo_japones_slug",
            attemptedProviders = null
        )
    }

    // ==================== ANIMEFLV PROVIDER ====================
    private fun tryAnimeFlvProvider(
        provider: ProviderConfig,
        tmdbTitle: String,
        tmdbYear: String?,
        normalizedType: String,
        season: Int?,
        episode: Int?,
        tmdbId: String
    ): ScrapeResponse? {
        // 1) Intenta AnimeFLV ahmedr API: https://animeflv.ahmedr.net/api/search?q=...
        // 2) Si falla, intenta scraping directo de animeflv.net
        // 3) Luego intenta obtener episodio y links
        val animeInfo = searchAnimeFlvAhmedr(tmdbTitle) ?: searchAnimeFlvDirect(tmdbTitle, provider.apis)
        if (animeInfo == null) return null
        val animeId = animeInfo.get("id")?.asText() ?: animeInfo.get("slug")?.asText() ?: animeInfo.get("_id")?.asText() ?: return null
        val animeSlug = animeInfo.get("slug")?.asText() ?: animeId
        val animeTitle = animeInfo.get("title")?.asText() ?: tmdbTitle
        val seasonNum = if (isSeriesType("animes")) season ?: 1 else null
        val episodeNum = if (isSeriesType("animes")) episode ?: 1 else null

        // Obtener links del episodio (AnimeFLV usa numeros de episodio, no seasons)
        val episodeLinks = fetchAnimeFlvEpisodeLinks(animeId, episodeNum ?: 1, provider)
        if (episodeLinks == null || episodeLinks.isEmpty()) return null
        // Buscar mediafire latino entre los links
        val mediafireCandidates = episodeLinks.filter { it.lowercase().contains("mediafire.com") }
        if (mediafireCandidates.isEmpty()) return null
        // AnimeFLV no suele tener campo lang, pero si hay múltiples, priorizamos mediafire
        val chosen = mediafireCandidates.first()
        val resolved = resolveMediafireOrDirect(chosen)
        val url = "${provider.baseUrl}/anime/$animeSlug"
        return ScrapeResponse(
            tmdbId = tmdbId,
            type = normalizedType,
            tmdbTitle = tmdbTitle,
            tmdbYear = tmdbYear,
            lamovieId = (animeId.hashCode().toLong() and 0x7fffffff),
            lamovieSlug = animeSlug,
            lamovieTitle = animeTitle,
            lamovieUrl = url,
            lamovieType = "animes",
            season = seasonNum,
            episode = episodeNum,
            episodeId = null,
            mediafire = MediafireDownloadInfo(chosen, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = episodeLinks.map { mapOf("url" to it, "lang" to "latino", "provider" to provider.name) },
            source = provider.name,
            sourceUrl = url,
            fallbackUsed = true,
            fallbackReason = "lamovie_solo_japones",
            attemptedProviders = null
        )
    }

    private fun tryAnimeFlvProviderBySlug(
        provider: ProviderConfig,
        slug: String,
        normalizedType: String,
        season: Int?,
        episode: Int?
    ): ScrapeResponse? {
        val animeInfo = fetchAnimeFlvBySlugAhmedr(slug) ?: return null
        val animeId = animeInfo.get("id")?.asText() ?: slug
        val animeTitle = animeInfo.get("title")?.asText() ?: slug
        val episodeNum = episode ?: 1
        val seasonNum = season ?: 1
        val episodeLinks = fetchAnimeFlvEpisodeLinks(animeId, episodeNum, provider) ?: return null
        val mediafireCandidates = episodeLinks.filter { it.lowercase().contains("mediafire.com") }
        if (mediafireCandidates.isEmpty()) return null
        val chosen = mediafireCandidates.first()
        val resolved = resolveMediafireOrDirect(chosen)
        val url = "${provider.baseUrl}/anime/$slug"
        return ScrapeResponse(
            tmdbId = slug,
            type = normalizedType,
            tmdbTitle = animeTitle,
            tmdbYear = null,
            lamovieId = (animeId.hashCode().toLong() and 0x7fffffff),
            lamovieSlug = slug,
            lamovieTitle = animeTitle,
            lamovieUrl = url,
            lamovieType = "animes",
            season = seasonNum,
            episode = episodeNum,
            episodeId = null,
            mediafire = MediafireDownloadInfo(chosen, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = episodeLinks.map { mapOf("url" to it, "lang" to "latino") },
            source = provider.name,
            sourceUrl = url,
            fallbackUsed = true,
            fallbackReason = "lamovie_solo_japones_slug",
            attemptedProviders = null
        )
    }

    private fun searchAnimeFlvAhmedr(query: String): JsonNode? {
        return try {
            val q = URLEncoder.encode(query, Charsets.UTF_8)
            val url = "$animeFlvAhmedrApi/search?query=$q"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
            val node = mapper.readTree(body)
            if (node.isArray && node.size() > 0) node.get(0)
            else if (node.has("data") && node.get("data").isArray && node.get("data").size() > 0) node.get("data").get(0)
            else if (node.has("animes") && node.get("animes").isArray && node.get("animes").size() > 0) node.get("animes").get(0)
            else null
        } catch (_: Exception) { null }
    }

    private fun fetchAnimeFlvBySlugAhmedr(slug: String): JsonNode? {
        return try {
            val url = "$animeFlvAhmedrApi/anime/$slug"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
            mapper.readTree(body)
        } catch (_: Exception) { null }
    }

    private fun searchAnimeFlvDirect(query: String, apis: List<String>): JsonNode? {
        for (api in apis) {
            try {
                val q = URLEncoder.encode(query, Charsets.UTF_8)
                val url = "$api/search?query=$q"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0"))
                val node = mapper.readTree(body)
                if (node.isArray && node.size() > 0) return node.get(0)
                if (node.has("data") && node.get("data").isArray && node.get("data").size() > 0) return node.get("data").get(0)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun fetchAnimeFlvEpisodeLinks(animeId: String, episode: Int, provider: ProviderConfig): List<String>? {
        // Intenta AnimeFLV ahmedr: /api/anime/{id}/episode/{ep}
        try {
            val url = "$animeFlvAhmedrApi/anime/$animeId/episode/$episode"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
            val node = mapper.readTree(body)
            // Busca array de servers
            val candidates = mutableListOf<String>()
            if (node.isArray) {
                for (n in node) {
                    val u = n.get("url")?.asText() ?: n.get("link")?.asText() ?: n.get("server")?.asText() ?: continue
                    candidates.add(u)
                }
            } else if (node.has("servers") && node.get("servers").isArray) {
                for (n in node.get("servers")) {
                    val u = n.get("url")?.asText() ?: n.get("link")?.asText() ?: continue
                    candidates.add(u)
                }
            } else if (node.has("data") && node.get("data").isArray) {
                for (n in node.get("data")) {
                    val u = n.get("url")?.asText() ?: continue
                    candidates.add(u)
                }
            }
            if (candidates.isNotEmpty()) return candidates
        } catch (_: Exception) {}
        // Intenta scrapear directo animeflv.net
        try {
            val url = "${provider.baseUrl}/ver/$animeId-$episode"
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            val regex = Regex("""https?://[^"\s']+mediafire\.com[^"\s']+""")
            val found = regex.findAll(html).map { it.value }.toList()
            if (found.isNotEmpty()) return found
        } catch (_: Exception) {}
        return null
    }

    // ==================== CONSUMET PROVIDER (Gogoanime / Zoro / Animeflix) ====================
    private fun tryConsumetProvider(
        provider: ProviderConfig,
        tmdbTitle: String,
        tmdbYear: String?,
        normalizedType: String,
        season: Int?,
        episode: Int?,
        tmdbId: String
    ): ScrapeResponse? {
        // Consumet Gogoanime: https://api.consumet.org/anime/gogoanime/{query}
        // Primero busca, luego info, luego watch para obtener sources
        // Para latino, filtramos subOrDub == dub
        val searchResults = searchConsumet(tmdbTitle, provider.apis[0])
        if (searchResults == null || searchResults.isEmpty()) return null
        // Prioriza dub (latino) si existe
        val dubResults = searchResults.filter { (it.get("subOrDub")?.asText()?.lowercase() ?: "") == "dub" || (it.get("title")?.asText()?.lowercase() ?: "").contains("dub") }
        val chosenAnime = if (dubResults.isNotEmpty()) dubResults[0] else searchResults[0]
        val animeId = chosenAnime.get("id")?.asText() ?: return null
        val animeTitle = chosenAnime.get("title")?.asText() ?: tmdbTitle
        val info = fetchConsumetInfo(animeId, provider.apis[0]) ?: return null
        val episodes = info.get("episodes")
        if (episodes == null || !episodes.isArray || episodes.size() == 0) return null
        val epNum = episode ?: 1
        // Episodios en consumet son 1-indexed por anime, no por temporada. Para series con season, calculamos offset simple.
        // Si season >1, aproximamos ep = (season-1)*12 + episode (heurística). Mejor buscar por título + season.
        var targetEpisodeId: String? = null
        for (ep in episodes) {
            val num = ep.get("number")?.asInt() ?: ep.get("episode")?.asInt() ?: continue
            if (num == epNum) {
                targetEpisodeId = ep.get("id")?.asText() ?: ep.get("episodeId")?.asText()
                break
            }
        }
        if (targetEpisodeId == null) {
            // toma el primero cercano
            val first = episodes.get(0)
            targetEpisodeId = first.get("id")?.asText() ?: first.get("episodeId")?.asText() ?: return null
        }
        val sources = fetchConsumetWatch(targetEpisodeId, provider.apis[0])
        if (sources == null) return null
        // Consumet devuelve m3u8, no mediafire. Para nuestro caso, usamos el m3u8 como directUrl (stream)
        // Pero mantenemos compatibilidad MediafireDownloadInfo con directUrl = m3u8
        val direct = sources.get("sources")?.let { arr ->
            if (arr.isArray && arr.size() > 0) arr.get(0).get("url")?.asText() else null
        } ?: sources.get("url")?.asText() ?: sources.get("link")?.asText() ?: return null
        val headers = sources.get("headers")?.toString()
        val url = chosenAnime.get("url")?.asText() ?: "${provider.baseUrl}/$animeId"
        return ScrapeResponse(
            tmdbId = tmdbId,
            type = normalizedType,
            tmdbTitle = tmdbTitle,
            tmdbYear = tmdbYear,
            lamovieId = (animeId.hashCode().toLong() and 0x7fffffff),
            lamovieSlug = animeId,
            lamovieTitle = animeTitle,
            lamovieUrl = url,
            lamovieType = "animes",
            season = season,
            episode = episode,
            episodeId = null,
            mediafire = MediafireDownloadInfo(direct, direct, null, "1080p", "latino", null),
            allDownloads = listOf(mapOf("url" to direct, "headers" to (headers ?: ""), "provider" to provider.name)),
            source = provider.name,
            sourceUrl = url,
            fallbackUsed = true,
            fallbackReason = "lamovie_solo_japones_consumet",
            attemptedProviders = null
        )
    }

    private fun tryConsumetProviderBySlug(
        provider: ProviderConfig,
        slug: String,
        normalizedType: String,
        season: Int?,
        episode: Int?
    ): ScrapeResponse? {
        val query = slug.replace("-", " ")
        return tryConsumetProvider(provider, query, null, normalizedType, season, episode, slug)
    }

    private fun searchConsumet(query: String, baseApi: String): List<JsonNode>? {
        return try {
            val q = URLEncoder.encode(query, Charsets.UTF_8)
            val url = "$baseApi/$q"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
            val node = mapper.readTree(body)
            when {
                node.has("results") && node.get("results").isArray -> node.get("results").toList()
                node.isArray -> node.toList()
                node.has("data") && node.get("data").isArray -> node.get("data").toList()
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun fetchConsumetInfo(animeId: String, baseApi: String): JsonNode? {
        return try {
            val url = "$baseApi/info/$animeId"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0"))
            mapper.readTree(body)
        } catch (_: Exception) { null }
    }

    private fun fetchConsumetWatch(episodeId: String, baseApi: String): JsonNode? {
        return try {
            val url = "$baseApi/watch/$episodeId"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0"))
            mapper.readTree(body)
        } catch (_: Exception) { null }
    }

    // ==================== GENERIC LAMOVIE-LIKE HELPERS ====================
    private fun searchLamovieLike(title: String, fastApis: List<String>): List<JsonNode> {
        val q = URLEncoder.encode(title, Charsets.UTF_8)
        val all = mutableListOf<JsonNode>()
        val seen = mutableSetOf<String>()
        for (fastApi in fastApis) {
            try {
                // Soporta ambos formatos: /wp-api/v1/search y /api/search
                val candidates = listOf(
                    "$fastApi/search?filter=%5B%5D&postType=any&q=$q&postsPerPage=10",
                    "$fastApi/search?q=$q",
                    "$fastApi/anime/search?query=$q"
                )
                var body: String? = null
                for (url in candidates) {
                    try {
                        body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                        if (body != null) break
                    } catch (_: Exception) {}
                }
                if (body == null) continue
                val node = mapper.readTree(body)
                val data: JsonNode? = when {
                    node.has("data") -> node.get("data")
                    node.has("results") -> node.get("results")
                    node.isArray -> node
                    else -> null
                } ?: continue
                val posts: JsonNode = when {
                    data?.has("posts") == true -> data.get("posts")
                    data?.isArray == true -> data
                    data?.has("results") == true -> data.get("results")
                    else -> null
                } ?: continue
                if (!posts.isArray) continue
                for (p in posts) {
                    val id = p.get("_id")?.asText() ?: p.get("id")?.asText() ?: p.get("slug")?.asText() ?: continue
                    if (seen.add(id)) all.add(p)
                }
                if (all.size >= 10) break
            } catch (_: Exception) {}
        }
        return all
    }

    private fun fetchSingleBySlugLike(slug: String, postType: String, fastApis: List<String>): JsonNode? {
        val candidates = when (postType) {
            "movies" -> listOf("movies", "tvshows", "animes")
            "tvshows" -> listOf("tvshows", "animes", "movies")
            else -> listOf(postType, "movies", "tvshows", "animes")
        }.distinct()
        for (pt in candidates) {
            for (fastApi in fastApis) {
                try {
                    val urls = listOf(
                        "$fastApi/single/$pt?slug=$slug&postType=$pt",
                        "$fastApi/anime/$slug",
                        "$fastApi/info/$slug"
                    )
                    for (url in urls) {
                        try {
                            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                            val node = mapper.readTree(body)
                            val data = node.get("data") ?: node
                            if (data.isArray && data.size() == 0) continue
                            if (data.has("error") && data.get("error").asBoolean()) continue
                            if (data.has("title") || data.has("slug") || data.has("name")) return data
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun fetchPlayerLike(postId: Long, fastApis: List<String>): JsonNode? {
        for (fastApi in fastApis) {
            try {
                val urls = listOf(
                    "$fastApi/player?postId=$postId&demo=0",
                    "$fastApi/servers?slug=$postId",
                    "$fastApi/anime/servers?slug=$postId"
                )
                for (url in urls) {
                    try {
                        val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                        val node = mapper.readTree(body)
                        val data = node.get("data") ?: node
                        if (data.has("downloads") && data.get("downloads").isArray && data.get("downloads").size() > 0) return data
                        if (data.has("servers") && data.get("servers").isArray && data.get("servers").size() > 0) {
                            // Convert servers to downloads format
                            val downloads = mapper.createArrayNode()
                            for (s in data.get("servers")) {
                                val obj = mapper.createObjectNode()
                                obj.put("url", s.get("url")?.asText() ?: s.get("link")?.asText() ?: "")
                                obj.put("lang", s.get("lang")?.asText() ?: s.get("language")?.asText() ?: "")
                                obj.put("quality", s.get("quality")?.asText() ?: "")
                                downloads.add(obj)
                            }
                            val wrapper = mapper.createObjectNode()
                            wrapper.set("downloads", downloads)
                            return wrapper
                        }
                        if (!data.has("error") || !data.get("error").asBoolean()) {
                            if (data.has("downloads") || data.has("url")) return data
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun findEpisodeLike(seriesId: Long, season: Int, episode: Int, fastApis: List<String>): JsonNode? {
        for (fastApi in fastApis) {
            try {
                val url = "$fastApi/single/episodes/list?_id=$seriesId&season=$season&page=1&postsPerPage=50"
                val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
                val node = mapper.readTree(body)
                val data = node.get("data") ?: continue
                val posts = data.get("posts") ?: continue
                if (!posts.isArray || posts.size() == 0) continue
                for (ep in posts) {
                    val s = ep.get("season_number")?.asInt() ?: ep.get("season")?.asInt()
                    val e = ep.get("episode_number")?.asInt() ?: ep.get("episode")?.asInt()
                    if (s == season && e == episode) return ep
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun pickBestPost(posts: List<JsonNode>, tmdbTitle: String, tmdbYear: String?, type: String): JsonNode? {
        val normTitle = normalize(tmdbTitle)
        var best: JsonNode? = null
        var bestScore = -1
        for (p in posts) {
            val title = p.get("title")?.asText() ?: p.get("name")?.asText() ?: ""
            val orig = p.get("original_title")?.asText() ?: ""
            val slug = p.get("slug")?.asText() ?: p.get("id")?.asText() ?: ""
            val releaseDate = p.get("release_date")?.asText() ?: p.get("year")?.asText() ?: ""
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

    private fun isSeriesType(lamovieType: String) = lamovieType.lowercase() in setOf("tvshows", "series", "tv", "animes", "anime")
    private fun isAnimeType(lamovieType: String) = lamovieType.lowercase() in setOf("animes", "anime")

    private fun getBaseUrlForType(lamovieType: String?, baseUrl: String): String = baseUrl
    private fun buildUrl(base: String, slug: String, lamovieType: String): String {
        val prefix = when {
            isAnimeType(lamovieType) -> "animes"
            isSeriesType(lamovieType) -> "series"
            else -> "peliculas"
        }
        return "$base/$prefix/$slug/"
    }

    private fun priority(n: JsonNode): Int {
        val u = (if (n.has("url_raw")) n.get("url_raw").asText() else n.get("url")?.asText() ?: "").lowercase()
        return when {
            u.contains("mediafire.com") -> 0
            u.contains("mega.nz") -> 1
            u.startsWith("magnet:") || u.contains(".torrent") -> 2
            else -> 3
        }
    }

    private fun resolveMediafireOrDirect(mediafireUrl: String): MediafireResolveResponse {
        if (!mediafireUrl.lowercase().contains("mediafire.com")) {
            // Para consumet / streams, directUrl es el mismo
            return MediafireResolveResponse(mediafireUrl, mediafireUrl, null)
        }
        return try {
            val body = httpGetString(mediafireUrl, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            var direct = Regex("""aria-label="Download file"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""id="downloadButton"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""href="(https://download[^"]+mediafire[^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""href="(https://download[^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = mediafireUrl
            else direct = direct.replace("&amp;", "&")
            val filename = try {
                val uri = URI(direct)
                uri.path.substringAfterLast("/").takeIf { it.isNotBlank() }
            } catch (_: Exception) { null }
            MediafireResolveResponse(mediafireUrl, direct, filename)
        } catch (_: Exception) {
            MediafireResolveResponse(mediafireUrl, mediafireUrl, null)
        }
    }

    private fun httpGetString(url: String, headers: Map<String, String>): String {
        // Intenta con HttpClient primero (con UA de navegador real para Cloudflare)
        val enrichedHeaders = mutableMapOf<String, String>()
        enrichedHeaders["User-Agent"] = headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        enrichedHeaders["Accept"] = headers["Accept"] ?: "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        enrichedHeaders["Accept-Language"] = headers["Accept-Language"] ?: "es-ES,es;q=0.9,en;q=0.8"
        enrichedHeaders["Referer"] = headers["Referer"] ?: "https://www.google.com/"
        headers.forEach { (k,v) -> if (k !in enrichedHeaders) enrichedHeaders[k] = v; else if (headers.containsKey(k)) enrichedHeaders[k]=v }
        // Si es latanime/jkanime/monoschinos añade más headers browser
        if (url.contains("latanime.org") || url.contains("jkanime") || url.contains("monoschinos")) {
            enrichedHeaders["Cache-Control"] = "no-cache"
            enrichedHeaders["Pragma"] = "no-cache"
        }
        try {
            val builder = HttpRequest.newBuilder(URI(url)).GET().timeout(Duration.ofSeconds(20))
            enrichedHeaders.forEach { (k, v) -> builder.header(k, v) }
            val request = builder.build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                return response.body()
            }
            if (response.statusCode() == 404) throw RuntimeException("HTTP 404 for $url")
            // Para 403/503 (Cloudflare) intenta curl fallback
            if (response.statusCode() in setOf(403,503,429,520,521,522,523,524)) {
                println("[AnimeFallback] HttpClient bloqueado $url -> ${response.statusCode()}, intenta curl fallback")
            } else {
                throw RuntimeException("Error HTTP ${response.statusCode()} al consultar $url: ${response.body().take(300)}")
            }
        } catch (e: Exception) {
            if (e.message?.contains("HTTP 404") == true) throw e
            println("[AnimeFallback] HttpClient error $url: ${e.message}, intenta curl fallback")
        }
        // Fallback via curl (más compatible con Cloudflare)
        return try {
            val cmd = arrayOf("curl", "-s", "-L", "-A", enrichedHeaders["User-Agent"]!!, "-H", "Accept: ${enrichedHeaders["Accept"]}", "-H", "Accept-Language: ${enrichedHeaders["Accept-Language"]}", url)
            val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
            pb.environment()["LC_ALL"] = "C"
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) { proc.destroyForcibly(); throw RuntimeException("curl timeout $url") }
            if (proc.exitValue() != 0) throw RuntimeException("curl failed ${proc.exitValue()} for $url: ${output.take(200)}")
            if (output.contains("<title>Just a moment") || output.contains("Checking if the site connection is secure")) {
                throw RuntimeException("Cloudflare challenge for $url")
            }
            if (output.isBlank()) throw RuntimeException("curl vacío para $url")
            output
        } catch (e2: Exception) {
            throw RuntimeException("HTTP fallo HttpClient+curl para $url: ${e2.message}")
        }
    }
}

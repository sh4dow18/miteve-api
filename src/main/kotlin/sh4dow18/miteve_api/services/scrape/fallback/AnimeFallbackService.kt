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
        // NOTA: la puerta "¿es anime?" vive en el llamador (LamovieScrapeService usa el
        // flag isAnime de TMDB) y en el match estricto de título de cada proveedor.
        // Este chain nunca devuelve contenido ajeno: sin match de título, sin temporada
        // pedida o sin latino verificado, cada proveedor devuelve null (404).
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
        // Estricto latino para películas, series y anime.
        val lang = resp.mediafire.lang?.lowercase()?.trim() ?: ""
        if (lang.isBlank()) return false
        return lang.contains("latino") || lang.contains("latin") || lang.contains("latín") || lang.contains("dobl")
    }

    fun cleanFallbackUrl(raw: String): String {
        var u = raw.trim().replace(Regex("[\\r\\n\\t\\u0000-\\u001F\\u007F]"), "").trim()
        u = u.trim('"', '\'', '<', '>', '(', ')', '[', ']')
        while (u.endsWith(".") || u.endsWith(",") || u.endsWith(";") || u.endsWith("!") || u.endsWith(")")) {
            u = u.dropLast(1).trim()
        }
        u = u.replace(" ", "%20")
        return u.trim()
    }

    // ¿El slug corresponde de verdad al título pedido? Exige solape de tokens
    // significativos (len>=4). Sin esto, "juego de tronos" matchearía cualquier anime
    // de la búsqueda y se descargarían archivos ajenos. Sin match -> null (404).
    fun titleMatchesSlug(slug: String, title: String): Boolean {
        fun fold(s: String) = s.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n").replace("ü", "u")
        val normTitleFull = fold(title).replace(Regex("[^a-z0-9]"), "")
        val normSlugFull = fold(slug).replace(Regex("[^a-z0-9]"), "")
        // Títulos cortos (sin tokens len>=4): exige contención completa.
        val titleTokens = fold(title)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 4 }
            .toSet()
        if (titleTokens.isEmpty()) {
            return normTitleFull.length >= 3 &&
                (normSlugFull.contains(normTitleFull) || normTitleFull.contains(normSlugFull))
        }
        val slugTokens = fold(slug).replace(Regex("[^a-z0-9]"), " ")
            .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        if (slugTokens.isEmpty()) return false
        for (tt in titleTokens) {
            for (st in slugTokens) {
                // Contención con solape mínimo de 4 caracteres (cubre "konosuba"~"kono").
                if (st.length >= 4 && tt.contains(st)) return true
                if (tt.length >= 4 && st.contains(tt)) return true
            }
        }
        return false
    }

    // Extrae el primer mediafire cuyo entorno (±1500 chars) mencione latino/doblado.
    // Evita tomar un link japonés de una página que menciona "latino" en otra sección
    // (filtros del sitio, otros animes, etc.).
    fun extractMediafireWithLatinoNearby(html: String): String? {
        val lower = html.lowercase()
        val directRegex = Regex("""https?://[^"\s'<>]+mediafire\.com[^"\s'<>]+""")
        for (m in directRegex.findAll(html)) {
            val start = maxOf(0, m.range.first - 1500)
            val end = minOf(html.length, m.range.last + 1500)
            val ctx = lower.substring(start, end)
            if (ctx.contains("latino") || ctx.contains("latin") || ctx.contains("latín") || ctx.contains("dobl")) {
                return cleanFallbackUrl(m.value)
            }
        }
        return null
    }

    fun isKonosubaLike(text: String): Boolean {
        val l = text.lowercase()
        return l.contains("konosuba") || l.contains("kono-subarashii") || l.contains("kono subarashii") || l.contains("shukufuku")
    }

    // Candidatos de slug latanime para la temporada pedida. Latanime separa temporadas
    // ("x-latino", "x-2-latino", "x-s3-latino"...). Si el slug encontrado es de otra
    // temporada, se deriva/prueba la variante correcta; si ninguna tiene latino es 404.
    // Jamás se devuelve otra temporada como si fuera la pedida.
    fun latanimeSeasonCandidates(bestSlug: String, season: Int?): List<String> {
        val s = season ?: 1
        val withLatino = if (bestSlug.endsWith("-latino")) bestSlug else "$bestSlug-latino"
        if (s <= 1) {
            val m = Regex("^(.*)-(2|3|s2|s3|temporada-[23]|[4-9])-latino$").find(withLatino)
            // Slug S2+ para pedido S1: prueba la base S1; si no existe, 404.
            if (m != null) return listOf("${m.groupValues[1]}-latino")
            return listOf(withLatino)
        }
        val markers = when (s) {
            2 -> listOf("-2-latino", "temporada-2")
            3 -> listOf("-3-latino", "s3-latino", "-s3-", "temporada-3")
            else -> listOf("-${s}-latino", "-s${s}-latino", "temporada-${s}")
        }
        if (markers.any { withLatino.contains(it) }) return listOf(withLatino)
        var base = withLatino.removeSuffix("-latino")
        base = base.replace(Regex("-(2|3|s2|s3|temporada-[23]|[4-9])$"), "")
        return listOf("$base-$s-latino", "$base-s$s-latino", "$base-temporada-$s-latino").distinct()
    }

    // Variantes de slug de temporada para sitios tipo JKanime/Monoschinos donde cada
    // temporada es una entrada separada ("spy-x-family-2", "-3", ...). NUNCA se devuelve
    // la S1 cuando se pidió S>1: si ninguna variante tiene latino, es 404.
    fun seasonSlugVariants(baseSlug: String, season: Int?): List<String> {
        if (season == null || season <= 1) return listOf(baseSlug)
        if (baseSlug.endsWith("-$season")) return listOf(baseSlug)
        val ordinal = when (season) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${season}th"
        }
        return listOf(
            "$baseSlug-$season",
            "$baseSlug-$ordinal-season",
            "$baseSlug-temporada-$season",
            "$baseSlug-s$season"
        ).distinct().filter { it != baseSlug }
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
        // Si piden temporada >1, busca también con la temporada para descubrir el slug
        // de esa temporada directamente (p. ej. "spy x family 3").
        if ((season ?: 1) > 1) {
            candidates.add("$baseQuery $season")
            candidates.add("$baseQuery temporada $season")
        }
        var slugs: List<Pair<String,String>> = emptyList()
        var usedQuery: String? = null
        for (q in candidates.filter { it.length >= 3 }.take(7)) {
            val res = searchJkanimeHtml(q)
            if (res.isNotEmpty()) { slugs = res; usedQuery = q; break }
        }
        if (slugs.isEmpty()) {
            // último intento con slug directo mapeado
            val aliasSlug = baseQuery.lowercase().replace("konosuba", "kono subarashii").replace("gods blessing", "sekai ni shukufuku")
            slugs = searchJkanimeHtml(aliasSlug.split(" ")[0])
            if (slugs.isEmpty()) return null
        }
        // Pick best slug via similitud con título TMDB (estricto: sin match -> null).
        val bestSlug = pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: return null
        val epNum = episode ?: 1
        // Temporadas: prueba variantes de slug de la temporada pedida. Si la S pedida no
        // tiene latino, es 404: JAMÁS se devuelve la S1 cuando se pidió S>1 (antes lo
        // hacía y descargaba la primera temporada en vez de la pedida).
        val seasonSlugs = if ((season ?: 1) <= 1) listOf(bestSlug) else seasonSlugVariants(bestSlug, season)
        var mediafireUrl: String? = null
        var usedSlug = seasonSlugs.first()
        for (sSlug in seasonSlugs) {
            // Solo slugs que sigan correspondiendo al título (evita saltar a otra serie).
            if (!titleMatchesSlug(sSlug, tmdbTitle) && sSlug != bestSlug) continue
            val found = fetchJkanimeMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$jkanimeBaseUrl/$usedSlug/"
        val title = slugs.find { it.first == bestSlug }?.second ?: tmdbTitle
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = title,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "jkanime")),
            source = "jkanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_jkanime_html", attemptedProviders = null
        )
    }

    private fun tryJkanimeHtmlProviderBySlug(
        slug: String, normalizedType: String, season: Int?, episode: Int?
    ): ScrapeResponse? {
        // Para slug de lamovie, intenta mapeo directo a jkanime slug via búsqueda por slug normalizado
        val query = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val slugs = searchJkanimeHtml(query)
        val bestSlug = if (slugs.isNotEmpty()) pickBestJkanimeSlug(slugs, query, null) ?: return null else slug.replace("konosuba", "kono-subarashii").replace("gods-blessing-on-this-wonderful-world", "sekai-ni-shukufuku-wo")
        val epNum = episode ?: 1
        val seasonSlugs = if ((season ?: 1) <= 1) listOf(bestSlug) else seasonSlugVariants(bestSlug, season)
        var mediafireUrl: String? = null
        var usedSlug = seasonSlugs.first()
        for (sSlug in seasonSlugs) {
            val found = fetchJkanimeMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$jkanimeBaseUrl/$usedSlug/"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "jkanime")),
            source = "jkanime", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_slug_jkanime_html", attemptedProviders = null
        )
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
        // Primero filtra por coincidencia real título<->slug. Sin match (p. ej. buscar
        // "juego de tronos" en un sitio de anime) -> null, nunca un anime ajeno.
        val matching = slugs.filter { (slug, _) -> titleMatchesSlug(slug, tmdbTitle) }
        if (matching.isEmpty()) return null
        val normTitle = normalize(tmdbTitle)
        var best: Pair<String,String>? = null
        var bestScore = -1
        for ((slug, title) in matching) {
            var score = 0
            val normSlug = normalize(slug.replace("-", ""))
            val normT = normalize(title)
            if (normSlug.contains(normTitle) || normTitle.contains(normSlug)) score += 10
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
        val konosubaLike = isKonosubaLike(tmdbTitle)
        // Filtra por temporada: S1 prefiere sin -2/-3, S2 prefiere -2-, S3 prefiere -3- o s3.
        // Los rescates "konosuba" solo aplican si el título pedido es konosuba.
        val seasonFiltered = when (season) {
            null, 1 -> slugs.filter { !it.first.contains("-2-") && !it.first.contains("-3-") && !it.first.contains("s2") && !it.first.contains("s3") && !it.first.contains("-ova") }
                .ifEmpty { if (konosubaLike) slugs.filter { it.first == "konosuba-latino" } else emptyList() } .ifEmpty { slugs }
            2 -> slugs.filter { it.first.contains("-2-") || it.first.contains("2-latino") }
                .ifEmpty { if (konosubaLike) slugs.filter { it.first.contains("konosuba") } else emptyList() }
            3 -> slugs.filter { it.first.contains("-3-") || it.first.contains("s3") || it.first.contains("3-latino") }
                .ifEmpty { if (konosubaLike) slugs.filter { it.first.contains("konosuba") } else emptyList() }
            else -> slugs.filter { it.first.contains("-${season}-") }
        }
        val candidates = if (seasonFiltered.isNotEmpty()) seasonFiltered else slugs
        // Atajos Konosuba: solo si el título pedido es konosuba.
        if (konosubaLike && (season == 1 || season == null)) {
            candidates.find { it.first == "konosuba-latino" }?.let { return it.first }
            candidates.find { it.first.contains("konosuba") && !it.first.contains("-2") && !it.first.contains("-3") }?.let { return it.first }
        }
        if (konosubaLike && season == 2) {
            candidates.find { it.first == "konosuba-2-latino" }?.let { return it.first }
        }
        if (konosubaLike && season == 3) {
            candidates.find { it.first.contains("s3-latino") }?.let { return it.first }
            candidates.find { it.first == "konosuba-3-latino" }?.let { return it.first }
        }
        return pickBestJkanimeSlug(candidates, tmdbTitle, null)
    }

    private fun fetchJkanimeMediafireLatino(slug: String, episode: Int): String? {
        return try {
            // JKanime episodio URL: https://jkanime.net/{slug}/{episode}/
            // Solo URLs de episodio: la página principal del anime (/$slug/) podría traer
            // links de otro episodio y se descargaría el episodio equivocado.
            val urlsToTry = listOf(
                "$jkanimeBaseUrl/$slug/$episode/",
                "$jkanimeBaseUrl/$slug/$episode"
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
                    val decoded = cleanFallbackUrl(String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8))
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
                        val decoded = cleanFallbackUrl(String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8))
                        if (decoded.lowercase().contains("mediafire.com")) candidates.add(decoded to lang)
                    } catch (_: Exception) {}
                }
            }
            // Sin JSON de servidores no se puede verificar el idioma: aunque la página
            // mencione "latino" en otra sección (filtros, otros animes), el link podría
            // ser japonés subtitulado. Solo se acepta lang==3 (español latino JKanime).
            if (candidates.isEmpty()) return null
            // SOLO lang 3 = español latino. lang 1 = japonés subtitulado, lang 2 = otro
            // (castellano/inglés según el caso): jamás se devuelven como latino.
            val latinoCandidates = candidates.filter { (_, lang) -> lang == 3 }
            val chosenRaw = latinoCandidates.firstOrNull()?.first ?: return null
            val chosen = cleanFallbackUrl(chosenRaw)
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
        val bestSlug = pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: return null
        val epNum = episode ?: 1
        // Temporadas: cada temporada es una entrada separada; sin variante con latino
        // para la S pedida es 404 (nunca S1 por S>1).
        val seasonSlugs = if ((season ?: 1) <= 1) listOf(bestSlug) else seasonSlugVariants(bestSlug, season)
        var mediafireUrl: String? = null
        var usedSlug = seasonSlugs.first()
        for (sSlug in seasonSlugs) {
            if (!titleMatchesSlug(sSlug, tmdbTitle) && sSlug != bestSlug) continue
            val found = fetchMonoschinosMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$monoschinosBaseUrl/anime/$usedSlug"
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = usedSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "monoschinos")),
            source = "monoschinos", sourceUrl = url, fallbackUsed = true, fallbackReason = "lamovie_solo_japones_monoschinos_html", attemptedProviders = null
        )
    }

    private fun tryMonoschinosHtmlProviderBySlug(
        slug: String, normalizedType: String, season: Int?, episode: Int?
    ): ScrapeResponse? {
        val query = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val slugs = searchMonoschinosHtml(query)
        val bestSlug = if (slugs.isNotEmpty()) pickBestJkanimeSlug(slugs, query, null) ?: return null else slug
        val epNum = episode ?: 1
        val seasonSlugs = if ((season ?: 1) <= 1) listOf(bestSlug) else seasonSlugVariants(bestSlug, season)
        var mediafireUrl: String? = null
        var usedSlug = seasonSlugs.first()
        for (sSlug in seasonSlugs) {
            val found = fetchMonoschinosMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$monoschinosBaseUrl/anime/$usedSlug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "monoschinos")),
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
            // Solo vale si hay un mediafire con "latino/doblado" CERCA del link (misma
            // sección de servidores). Un "latino" en otra parte de la página (filtros,
            // otros animes) no valida un link japonés subtitulado.
            return extractMediafireWithLatinoNearby(html)
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
        val bestSlug = pickBestLatanimeSlugForSeason(slugs, tmdbTitle, season) ?: pickBestJkanimeSlug(slugs, tmdbTitle, tmdbYear) ?: return null
        // Latanime usa sufijos -latino, -2-latino etc. Ya viene con -latino en el slug
        var targetSlug = if (bestSlug.contains("-latino")) bestSlug else "$bestSlug-latino"
        // Correcciones konosuba: SOLO si el título pedido es konosuba. Para el resto se
        // respeta el slug encontrado y jamás se sustituye por otra serie.
        val konosubaLike = isKonosubaLike(tmdbTitle) || isKonosubaLike(bestSlug)
        if (konosubaLike && season == 1 && targetSlug.contains("-2-latino")) targetSlug = "konosuba-latino"
        if (konosubaLike && season == 1 && targetSlug.contains("-3-")) targetSlug = "konosuba-latino"
        if (konosubaLike && season == 2 && !targetSlug.contains("-2-")) targetSlug = "konosuba-2-latino"
        if (konosubaLike && season == 3 && !targetSlug.contains("-3-") && !targetSlug.contains("s3")) targetSlug = "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
        val epNum = episode ?: 1
        // Latanime episodio: /ver/{slug}-episodio-{ep}. Se prueban variantes de la
        // temporada pedida; si ninguna tiene latino, es 404 (nunca S1 por S3).
        var mediafireUrl: String? = null
        var usedSlug = targetSlug
        for (sSlug in latanimeSeasonCandidates(targetSlug, season)) {
            val found = fetchLatanimeMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$latanimeBaseUrl/anime/$usedSlug"
        val title = slugs.find { it.first == bestSlug }?.second ?: tmdbTitle
        return ScrapeResponse(
            tmdbId = tmdbId, type = normalizedType, tmdbTitle = tmdbTitle, tmdbYear = tmdbYear,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = title,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "latanime")),
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
        if (isKonosubaLike(baseQuery) || isKonosubaLike(slug)) candidates.add("konosuba") // fallback final solo konosuba
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
        val bestSlug = if (slugs.isNotEmpty()) pickBestLatanimeSlugForSeason(slugs, baseQuery, season) ?: pickBestJkanimeSlug(slugs, baseQuery, null) ?: return null else {
            // último fallback alias (solo konosuba, verificado que existe)
            if (isKonosubaLike(baseQuery) || isKonosubaLike(slug)) {
                when (season) {
                    2 -> "konosuba-2-latino"
                    3 -> "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
                    else -> "konosuba-latino"
                }
            } else return null
        }
        // Ajusta para temporada: latanime usa -2-latino, -3-latino / s3.
        // Las correcciones konosuba solo aplican a konosuba; para el resto se respeta
        // el slug encontrado y jamás se sustituye por otra serie.
        var targetSlug = if (bestSlug.contains("-latino")) bestSlug else "$bestSlug-latino"
        val konosubaLike = isKonosubaLike(baseQuery) || isKonosubaLike(bestSlug) || isKonosubaLike(slug)
        // Corrección estricta por temporada solicitada (solo konosuba)
        if (konosubaLike && season == 1) {
            if (targetSlug.contains("-2-latino") || targetSlug.contains("-3-") || targetSlug.contains("s3")) {
                targetSlug = "konosuba-latino"
            }
        }
        if (konosubaLike && season == 2 && !targetSlug.contains("-2-")) {
            if (bestSlug == "konosuba-latino" || bestSlug.contains("konosuba")) targetSlug = "konosuba-2-latino"
        }
        if (konosubaLike && season == 3 && !targetSlug.contains("-3-") && !targetSlug.contains("s3")) {
            if (bestSlug == "konosuba-latino" || bestSlug.contains("konosuba")) targetSlug = "kono-subarashii-sekai-ni-shukufuku-wo-s3-latino"
        }
        val epNum = episode ?: 1
        // Solo variantes del slug pedido: si la temporada pedida no tiene latino, es 404.
        // Nunca se devuelve otra temporada ni otra serie (antes caía a konosuba-latino).
        var mediafireUrl: String? = null
        var usedSlug = targetSlug
        for (sSlug in latanimeSeasonCandidates(targetSlug, season)) {
            val found = fetchLatanimeMediafireLatino(sSlug, epNum)
            if (found != null) { mediafireUrl = found; usedSlug = sSlug; break }
        }
        val finalMediafireUrl = mediafireUrl ?: return null
        val resolved = resolveMediafireOrDirect(finalMediafireUrl)
        val url = "$latanimeBaseUrl/anime/$usedSlug"
        return ScrapeResponse(
            tmdbId = slug, type = normalizedType, tmdbTitle = bestSlug, tmdbYear = null,
            lamovieId = (usedSlug.hashCode().toLong() and 0x7fffffff), lamovieSlug = usedSlug, lamovieTitle = bestSlug,
            lamovieUrl = url, lamovieType = "animes", season = season, episode = episode, episodeId = null,
            mediafire = MediafireDownloadInfo(finalMediafireUrl, resolved.directUrl, resolved.filename, null, "latino", null),
            allDownloads = listOf(mapOf("url" to finalMediafireUrl, "lang" to "latino", "provider" to "latanime")),
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
            val candidates = directRegex.findAll(html).map { cleanFallbackUrl(it.value) }.toList()
            if (candidates.isEmpty()) return null
            // Latanime es 100% latino, cualquier mediafire es latino
            cleanFallbackUrl(candidates.first())
        } catch (_: Exception) { null }
    }

    // ==================== TIOANIME / ANIMEFENIX HTML (genérico) ====================
    private fun tryTioAnimeHtmlProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        // TioAnime no separa temporadas de forma fiable en la URL del episodio: si piden
        // S>1 se omite el proveedor antes que devolver la S1 como si fuera la S pedida.
        if ((season ?: 1) > 1) return null
        // TioAnime: estructura similar, busca via /directorio?q=
        val q = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim().split(" ")[0]
        val slugs = searchGenericHtml("$tioanimeBaseUrl/directorio?q=${URLEncoder.encode(q, Charsets.UTF_8)}", "/anime/([^\"/]+)")
        if (slugs.isEmpty()) return null
        val bestSlug = pickBestJkanimeSlug(slugs.map { it to it }, tmdbTitle, tmdbYear) ?: return null
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
        if ((season ?: 1) > 1) return null
        val q = slug.replace("-", " ").substringBefore(" ")
        val slugs = searchGenericHtml("$tioanimeBaseUrl/directorio?q=${URLEncoder.encode(q, Charsets.UTF_8)}", "/anime/([^\"/]+)")
        // Exige coincidencia real con el slug pedido; antes tomaba slugs[0] (ajeno).
        val bestSlug = if (slugs.isNotEmpty()) pickBestJkanimeSlug(slugs.map { it to it }, q, null) ?: return null else slug
        if (!titleMatchesSlug(bestSlug, q) && bestSlug != slug) return null
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
        if ((season ?: 1) > 1) return null
        val q = tmdbTitle.replace(Regex("\\(.*?\\)"), "").trim().split(" ")[0]
        val slugs = searchGenericHtml("$animefenixBaseUrl/?s=${URLEncoder.encode(q, Charsets.UTF_8)}", "/ver/([^\"/]+)")
        if (slugs.isEmpty()) return null
        // Exige coincidencia real; antes tomaba slugs[0] aunque fuera otra serie.
        val bestSlug = pickBestJkanimeSlug(slugs.map { it to it }, tmdbTitle, tmdbYear) ?: return null
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
        if ((season ?: 1) > 1) return null
        // Verifica que el slug exista de verdad en el sitio buscando el título; antes
        // descargaba "$slug" a ciegas aunque fuera de otra serie o no existiera.
        val q = slug.replace("-", " ").replace(Regex("\\b\\d{4}\\b"), "").trim()
        val found = searchGenericHtml("$animefenixBaseUrl/?s=${URLEncoder.encode(q, Charsets.UTF_8)}", "/ver/([^\"/]+)")
        if (found.isNotEmpty() && pickBestJkanimeSlug(found.map { it to it }, q, null) == null) return null
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
            // Solo vale un mediafire con "latino/doblado" CERCA del link; un "latino" en
            // otra parte de la página no valida un link japonés subtitulado.
            extractMediafireWithLatinoNearby(html)
        } catch (_: Exception) { null }
    }

    // ==================== NYAA TORRENT LATINO ====================
    private fun tryNyaaTorrentProvider(
        tmdbTitle: String, tmdbYear: String?, normalizedType: String, season: Int?, episode: Int?, tmdbId: String
    ): ScrapeResponse? {
        val query = buildNyaaQuery(tmdbTitle, season, episode)
        val torrentUrl = searchNyaaLatinoTorrent(query, tmdbTitle) ?: return null
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
        val torrentUrl = searchNyaaLatinoTorrent(query, slug.replace("-", " ")) ?: return null
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

    private fun searchNyaaLatinoTorrent(query: String, titleForMatch: String? = null): String? {
        return try {
            val url = "$nyaaBaseUrl/?f=0&c=1_2&q=$query"
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            // Busca magnet:?xt=urn:btih:...
            val magnetRegex = Regex("""magnet:\?xt=urn:btih:[^"\s'<>]+""")
            val magnets = magnetRegex.findAll(html).map { cleanFallbackUrl(it.value.replace("&amp;", "&")) }.toList()
            // Exige "latino" en el propio magnet (dn) y, si se conoce el título, que el
            // magnet también contenga el título: evita un torrent ajeno.
            var latinoMagnets = magnets.filter { it.lowercase().contains("latino") }
            if (titleForMatch != null) {
                val withTitle = latinoMagnets.filter {
                    val decoded = try { java.net.URLDecoder.decode(it, Charsets.UTF_8) } catch (_: Exception) { it }
                    titleMatchesSlug(decoded, titleForMatch)
                }
                // Si ninguno menciona el título, no es el contenido pedido -> null.
                if (withTitle.isEmpty()) return null
                latinoMagnets = withTitle
            }
            latinoMagnets.firstOrNull()
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
        // (solo con coincidencia real de título, nunca searchPosts[0] ajeno)
        val searchPosts = searchLamovieLike(tmdbTitle, provider.apis)
        if (searchPosts.isEmpty()) return null
        val best = pickBestPost(searchPosts, tmdbTitle, tmdbYear, normalizedType) ?: return null
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
        val latinoCandidates = mediafireCandidates.filter { n -> val l = n.get("lang")?.takeIf { !it.isNull }?.asText()?.lowercase()?.trim() ?: ""; l.contains("latino") || l.contains("latin") || l.contains("latín") || l.contains("dobl") }
        // Solo queremos latino. Si no hay latino, no retornamos (para que el chain continue)
        if (latinoCandidates.isEmpty()) return null
        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: return null
        val mediafireUrl = cleanFallbackUrl(if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText())
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
            // fallback a search (solo con coincidencia real de título, nunca posts[0] ajeno)
            val q = slug.replace("-", " ")
            val posts = searchLamovieLike(q, provider.apis)
            if (posts.isEmpty()) return null
            pickBestPost(posts, q, null, normalizedType) ?: return null
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
        val latinoCandidates = mediafireCandidates.filter { n -> val l = n.get("lang")?.takeIf { !it.isNull }?.asText()?.lowercase()?.trim() ?: ""; l.contains("latino") || l.contains("latin") || l.contains("latín") || l.contains("dobl") }
        if (latinoCandidates.isEmpty()) return null
        val mediafireNode = latinoCandidates.minByOrNull { priority(it) } ?: return null
        val mediafireUrl = cleanFallbackUrl(if (mediafireNode.has("url_raw")) mediafireNode.get("url_raw").asText() else mediafireNode.get("url").asText())
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
    // AnimeFLV (animeflv.net) es un sitio de contenido SUBTITULADO (audio japonés). Solo
    // devuelve algo si la API marca explícitamente un servidor como latino/doblado; si
    // no, es 404 en vez de etiquetar japonés subtitulado como "latino".
    private fun tryAnimeFlvProvider(
        provider: ProviderConfig,
        tmdbTitle: String,
        tmdbYear: String?,
        normalizedType: String,
        season: Int?,
        episode: Int?,
        tmdbId: String
    ): ScrapeResponse? {
        // AnimeFLV no resuelve temporadas (cada temporada es otro anime): S>1 -> null.
        if ((season ?: 1) > 1) return null
        // 1) Intenta AnimeFLV ahmedr API: https://animeflv.ahmedr.net/api/search?q=...
        // 2) Si falla, intenta scraping directo de animeflv.net
        // 3) Luego intenta obtener episodio y links
        val animeInfo = searchAnimeFlvAhmedr(tmdbTitle) ?: searchAnimeFlvDirect(tmdbTitle, provider.apis)
        if (animeInfo == null) return null
        val animeId = animeInfo.get("id")?.asText() ?: animeInfo.get("slug")?.asText() ?: animeInfo.get("_id")?.asText() ?: return null
        val animeSlug = animeInfo.get("slug")?.asText() ?: animeId
        val animeTitle = animeInfo.get("title")?.asText() ?: tmdbTitle
        // Exige que el anime encontrado corresponda al pedido (no otro anime ajeno).
        if (!titleMatchesSlug(animeSlug, tmdbTitle) && !titleMatchesSlug(animeTitle, tmdbTitle) && !titleMatchesSlug(animeId, tmdbTitle)) return null
        val seasonNum = if (isSeriesType("animes")) season ?: 1 else null
        val episodeNum = if (isSeriesType("animes")) episode ?: 1 else null

        // Obtener links del episodio (AnimeFLV usa numeros de episodio, no seasons)
        val episodeLinks = fetchAnimeFlvEpisodeLinks(animeId, episodeNum ?: 1, provider)
        if (episodeLinks == null || episodeLinks.isEmpty()) return null
        // Solo servidores marcados explícitamente como latino/doblado. AnimeFLV es
        // subtitulado por defecto: sin marca latino es japonés -> 404, nunca "latino".
        val latinoLinks = episodeLinks.filter { (_, lang) ->
            val l = (lang ?: "").lowercase()
            l.contains("latino") || l.contains("latin") || l.contains("latín") || l.contains("dobl") || l.contains("dub")
        }
        if (latinoLinks.isEmpty()) return null
        // Buscar mediafire latino entre los links verificados
        val mediafireCandidates = latinoLinks.map { it.first }.filter { it.lowercase().contains("mediafire.com") }
        if (mediafireCandidates.isEmpty()) return null
        // AnimeFLV no suele tener campo lang, pero si hay múltiples, priorizamos mediafire
        val chosen = cleanFallbackUrl(mediafireCandidates.first())
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
            allDownloads = episodeLinks.map { mapOf("url" to it.first, "lang" to (it.second ?: "latino"), "provider" to provider.name) },
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
        if ((season ?: 1) > 1) return null
        val animeInfo = fetchAnimeFlvBySlugAhmedr(slug) ?: return null
        val animeId = animeInfo.get("id")?.asText() ?: slug
        val animeTitle = animeInfo.get("title")?.asText() ?: slug
        if (!titleMatchesSlug(animeId, slug.replace("-", " ")) && !titleMatchesSlug(animeTitle, slug.replace("-", " ")) && animeId != slug) return null
        val episodeNum = episode ?: 1
        val seasonNum = season ?: 1
        val episodeLinks = fetchAnimeFlvEpisodeLinks(animeId, episodeNum, provider) ?: return null
        val latinoLinks = episodeLinks.filter { (_, lang) ->
            val l = (lang ?: "").lowercase()
            l.contains("latino") || l.contains("latin") || l.contains("latín") || l.contains("dobl") || l.contains("dub")
        }
        if (latinoLinks.isEmpty()) return null
        val mediafireCandidates = latinoLinks.map { it.first }.filter { it.lowercase().contains("mediafire.com") }
        if (mediafireCandidates.isEmpty()) return null
        val chosen = cleanFallbackUrl(mediafireCandidates.first())
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
            allDownloads = episodeLinks.map { mapOf("url" to it.first, "lang" to (it.second ?: "latino")) },
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

    private fun fetchAnimeFlvEpisodeLinks(animeId: String, episode: Int, provider: ProviderConfig): List<Pair<String, String?>>? {
        // Intenta AnimeFLV ahmedr: /api/anime/{id}/episode/{ep}
        // Devuelve pares (url, lang) para poder exigir latino verificado.
        try {
            val url = "$animeFlvAhmedrApi/anime/$animeId/episode/$episode"
            val body = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "application/json"))
            val node = mapper.readTree(body)
            // Busca array de servers
            val candidates = mutableListOf<Pair<String, String?>>()
            fun nodeLang(n: JsonNode): String? =
                n.get("lang")?.takeIf { !it.isNull }?.asText()
                    ?: n.get("language")?.takeIf { !it.isNull }?.asText()
                    ?: n.get("audio")?.takeIf { !it.isNull }?.asText()
                    ?: n.get("subOrDub")?.takeIf { !it.isNull }?.asText()
                    ?: n.get("type")?.takeIf { !it.isNull }?.asText()
            if (node.isArray) {
                for (n in node) {
                    val u = n.get("url")?.asText() ?: n.get("link")?.asText() ?: n.get("server")?.asText() ?: continue
                    candidates.add(cleanFallbackUrl(u) to nodeLang(n))
                }
            } else if (node.has("servers") && node.get("servers").isArray) {
                for (n in node.get("servers")) {
                    val u = n.get("url")?.asText() ?: n.get("link")?.asText() ?: continue
                    candidates.add(cleanFallbackUrl(u) to nodeLang(n))
                }
            } else if (node.has("data") && node.get("data").isArray) {
                for (n in node.get("data")) {
                    val u = n.get("url")?.asText() ?: continue
                    candidates.add(cleanFallbackUrl(u) to nodeLang(n))
                }
            }
            if (candidates.isNotEmpty()) return candidates
        } catch (_: Exception) {}
        // Intenta scrapear directo animeflv.net (sin info de idioma -> lang null,
        // los llamadores exigen marca latino así que esto normalmente será 404).
        try {
            val url = "${provider.baseUrl}/ver/$animeId-$episode"
            val html = httpGetString(url, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            val regex = Regex("""https?://[^"\s']+mediafire\.com[^"\s']+""")
            val found = regex.findAll(html).map { cleanFallbackUrl(it.value) to null }.toList()
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
        // Primero busca, luego info, luego watch para obtener sources.
        // REGLAS: solo DUB verificado (latino). El SUB es japonés subtitulado: jamás se
        // devuelve como latino (antes caía al sub y lo etiquetaba "latino"). S>1 -> null
        // porque los episodios de consumet son absolutos por anime, no por temporada.
        if ((season ?: 1) > 1) return null
        val searchResults = searchConsumet(tmdbTitle, provider.apis[0])
        if (searchResults == null || searchResults.isEmpty()) return null
        // Solo dub. Sin dub no hay latino en consumet -> 404.
        val dubResults = searchResults.filter { (it.get("subOrDub")?.asText()?.lowercase() ?: "") == "dub" || (it.get("title")?.asText()?.lowercase() ?: "").contains("dub") }
        if (dubResults.isEmpty()) return null
        val titleMatched = dubResults.filter {
            val cid = it.get("id")?.asText() ?: ""
            val ct = it.get("title")?.asText() ?: ""
            titleMatchesSlug(cid, tmdbTitle) || titleMatchesSlug(ct, tmdbTitle)
        }
        if (titleMatched.isEmpty()) return null
        val chosenAnime = titleMatched[0]
        val animeId = chosenAnime.get("id")?.asText() ?: return null
        val animeTitle = chosenAnime.get("title")?.asText() ?: tmdbTitle
        val info = fetchConsumetInfo(animeId, provider.apis[0]) ?: return null
        val episodes = info.get("episodes")
        if (episodes == null || !episodes.isArray || episodes.size() == 0) return null
        val epNum = episode ?: 1
        var targetEpisodeId: String? = null
        for (ep in episodes) {
            val num = ep.get("number")?.asInt() ?: ep.get("episode")?.asInt() ?: continue
            if (num == epNum) {
                targetEpisodeId = ep.get("id")?.asText() ?: ep.get("episodeId")?.asText()
                break
            }
        }
        // Sin el episodio exacto es 404: antes tomaba el primero (episodio ajeno).
        if (targetEpisodeId == null) return null
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
        // Mismo filtro estricto: movie -> movies+animes, tv -> tvshows+animes.
        val candidates = when (postType) {
            "movies" -> listOf("movies", "animes")
            "tvshows" -> listOf("tvshows", "animes")
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
            // Sin señal de título se ignora el post (evita devolver contenido ajeno).
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
        val cleanedInput = cleanFallbackUrl(mediafireUrl)
        if (!cleanedInput.lowercase().contains("mediafire.com")) {
            // Para consumet / streams, directUrl es el mismo
            return MediafireResolveResponse(cleanedInput, cleanFallbackUrl(cleanedInput), null)
        }
        return try {
            val body = httpGetString(cleanedInput, mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "text/html"))
            var direct = Regex("""aria-label="Download file"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""id="downloadButton"[^>]*href="([^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""href="(https://download[^"]+mediafire[^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = Regex("""href="(https://download[^"]+)"""").find(body)?.groupValues?.get(1)
            if (direct == null) direct = cleanedInput
            else direct = cleanFallbackUrl(direct.replace("&amp;", "&"))
            val filename = try {
                val uri = URI(direct)
                uri.path.substringAfterLast("/").takeIf { it.isNotBlank() }
            } catch (_: Exception) { null }
            MediafireResolveResponse(cleanedInput, direct, filename)
        } catch (_: Exception) {
            MediafireResolveResponse(cleanedInput, cleanedInput, null)
        }
    }

    private fun httpGetString(url: String, headers: Map<String, String>): String {
        // Limpia la URL para evitar "Illegal character in path" por espacios/saltos invisibles.
        val safeUrl = cleanFallbackUrl(url)
        // Intenta con HttpClient primero (con UA de navegador real para Cloudflare).
        // OJO: los callers pasan "Mozilla/5.0" genérico, que MediaFire/Cloudflare
        // bloquea con 403 "Just a moment...". Se fuerza UA completo salvo que el
        // caller ya traiga uno de navegador real.
        val fullUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        val fullAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        fun isGenericUa(ua: String?) = ua == null || ua.trim() == "Mozilla/5.0"
        val enrichedHeaders = mutableMapOf<String, String>()
        enrichedHeaders["User-Agent"] = headers["User-Agent"]?.takeIf { !isGenericUa(it) } ?: fullUa
        // Para mediafire se fuerza Accept de navegador (el "text/html" escueto ayuda al 403).
        enrichedHeaders["Accept"] = if (safeUrl.lowercase().contains("mediafire.com")) fullAccept
            else headers["Accept"] ?: fullAccept
        enrichedHeaders["Accept-Language"] = headers["Accept-Language"] ?: "es-ES,es;q=0.9,en;q=0.8"
        enrichedHeaders["Referer"] = headers["Referer"] ?: "https://www.google.com/"
        enrichedHeaders.putIfAbsent("Upgrade-Insecure-Requests", "1")
        enrichedHeaders.putIfAbsent("Sec-Fetch-Dest", "document")
        enrichedHeaders.putIfAbsent("Sec-Fetch-Mode", "navigate")
        enrichedHeaders.putIfAbsent("Sec-Fetch-Site", "none")
        headers.forEach { (k,v) ->
            if (k == "User-Agent" && isGenericUa(v)) return@forEach
            if (k == "Accept" && safeUrl.lowercase().contains("mediafire.com")) return@forEach
            if (k !in enrichedHeaders) enrichedHeaders[k] = v
        }
        // Si es latanime/jkanime/monoschinos añade más headers browser
        if (safeUrl.contains("latanime.org") || safeUrl.contains("jkanime") || safeUrl.contains("monoschinos")) {
            enrichedHeaders["Cache-Control"] = "no-cache"
            enrichedHeaders["Pragma"] = "no-cache"
        }
        try {
            val builder = HttpRequest.newBuilder(URI(safeUrl)).GET().timeout(Duration.ofSeconds(20))
            enrichedHeaders.forEach { (k, v) -> builder.header(k, v) }
            val request = builder.build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                val body = response.body()
                // Cloudflare a veces responde 200 con el challenge embebido:
                // no devolverlo como página real, caer al curl.
                if (body.contains("<title>Just a moment") || body.contains("Checking if the site connection is secure")) {
                    println("[AnimeFallback] HttpClient recibió challenge Cloudflare para $safeUrl, intenta curl fallback")
                } else {
                    return body
                }
            }
            if (response.statusCode() == 404) throw RuntimeException("HTTP 404 for $safeUrl")
            // Para 403/503 (Cloudflare) intenta curl fallback
            if (response.statusCode() in setOf(403,503,429,520,521,522,523,524)) {
                println("[AnimeFallback] HttpClient bloqueado $safeUrl -> ${response.statusCode()}, intenta curl fallback")
            } else {
                throw RuntimeException("Error HTTP ${response.statusCode()} al consultar $safeUrl: ${response.body().take(300)}")
            }
        } catch (e: Exception) {
            if (e.message?.contains("HTTP 404") == true) throw e
            println("[AnimeFallback] HttpClient error $safeUrl: ${e.message}, intenta curl fallback")
        }
        // Fallback via curl (más compatible con Cloudflare: --http2 + headers
        // Sec-Fetch-* verificados en vivo: MIN -> 403, FULL -> 200 en mediafire)
        return try {
            val cmd = arrayOf("curl", "-s", "-L", "--http2", "--compressed", "-A", enrichedHeaders["User-Agent"]!!, "-H", "Accept: ${enrichedHeaders["Accept"]}", "-H", "Accept-Language: ${enrichedHeaders["Accept-Language"]}", "-H", "Upgrade-Insecure-Requests: 1", "-H", "Sec-Fetch-Dest: document", "-H", "Sec-Fetch-Mode: navigate", "-H", "Sec-Fetch-Site: none", "-H", "Referer: ${enrichedHeaders["Referer"]}", "--max-time", "25", safeUrl)
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

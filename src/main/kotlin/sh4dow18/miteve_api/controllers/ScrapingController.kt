package sh4dow18.miteve_api.controllers

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.beans.factory.annotation.Autowired
import sh4dow18.miteve_api.services.scrape.AbstractLamovieScrapeService
import sh4dow18.miteve_api.services.scrape.ScrapeJobService
import sh4dow18.miteve_api.services.scrape.fallback.AnimeFallbackService
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@RestController
@RequestMapping("\${endpoint.scrape}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ScrapingController(
    private val scrapeService: AbstractLamovieScrapeService,
    private val scrapeJobService: ScrapeJobService,
    @Autowired(required = false) private val fallbackService: AnimeFallbackService? = null
) {

    /**
     * Scrape principal con fallback latino automático para anime.
     * Flujo: lamovie.la (primario) -> si solo japonés, fallback chain JKanime -> MonosChinos -> AnimeFLV -> Consumet (ver imagen)
     * Para animes donde lamovie solo tiene japonés (ej: Konosuba), el fallback ya descarga Mediafire latino.
     */
    @GetMapping("/{tmdbId}")
    fun scrape(
        @PathVariable tmdbId: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ) = scrapeService.scrape(tmdbId, type, season, episode)

    @GetMapping("/slug/{slug}")
    fun scrapeBySlug(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ) = scrapeService.scrapeBySlug(slug, type, season, episode)

    /**
     * Endpoint explícito latino: fuerza búsqueda en fallback si lamovie no tiene latino.
     * Útil para animes donde se quiere asegurar Mediafire latino.
     */
    @GetMapping("/{tmdbId}/latino")
    fun scrapeLatino(
        @PathVariable tmdbId: String,
        @RequestParam(defaultValue = "tv") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ) = scrapeService.scrape(tmdbId, type, season, episode)

    @GetMapping("/slug/{slug}/latino")
    fun scrapeBySlugLatino(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "tv") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ) = scrapeService.scrapeBySlug(slug, type, season, episode)

    @GetMapping("/fallback/providers")
    fun fallbackProviders(): Any {
        if (fallbackService == null) return mapOf("enabled" to false, "reason" to "fallbackService not available")
        return mapOf(
            "enabled" to fallbackService.isFallbackEnabled(),
            "providers" to listOf("jkanime", "latanime", "monoschinos", "tioanime", "animefenix", "animeflv", "consumet", "nyaa"),
            "order" to "lamovie.la -> jkanime -> latanime (100% latino, verificado Konosuba S1 mediafire) -> monoschinos -> tioanime -> animefenix -> animeflv -> consumet (+ jikan) -> nyaa (torrent latino 1080p)",
            "description" to "Para anime latino doblado: primero lamovie.la, si solo japonés (Sub español) se ignora y se busca latino en fallbacks. JKanime (lang 3 latino), Latanime.org (/buscar?q= -> /anime/{slug} -> /ver/{slug}-episodio-{ep} mediafire latino verificado), MonosChinos, TioAnime, AnimeFenix, AnimeFLV (ahmedr), Consumet (gogoanime/zoro) y Nyaa torrent Latino. Si ninguno tiene latino tras probar todos, error 404 solo latino (como Konosuba ahora 10/10 via latanime)."
        )
    }

    @GetMapping("/{tmdbId}/download")
    fun downloadToServer(
        @PathVariable tmdbId: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?,
        @RequestParam(defaultValue = "false") async: Boolean
    ): Any {
        return if (async) scrapeJobService.startDownloadJob(tmdbId, type, season, episode)
        else scrapeService.downloadToServer(tmdbId, type, season, episode)
    }

    @PostMapping("/{tmdbId}/download")
    fun downloadToServerPost(
        @PathVariable tmdbId: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?,
        @RequestParam(defaultValue = "false") async: Boolean
    ): Any {
        return if (async) scrapeJobService.startDownloadJob(tmdbId, type, season, episode)
        else scrapeService.downloadToServer(tmdbId, type, season, episode)
    }

    @GetMapping("/slug/{slug}/download")
    fun downloadBySlug(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?,
        @RequestParam(defaultValue = "false") async: Boolean
    ): Any {
        return if (async) scrapeJobService.startDownloadJob(slug, type, season, episode)
        else scrapeService.downloadBySlugToServer(slug, type, season, episode)
    }

    @PostMapping("/slug/{slug}/download")
    fun downloadBySlugPost(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?,
        @RequestParam(defaultValue = "false") async: Boolean
    ): Any {
        return if (async) scrapeJobService.startDownloadJob(slug, type, season, episode)
        else scrapeService.downloadBySlugToServer(slug, type, season, episode)
    }

    @GetMapping("/job/{jobId}")
    fun getDownloadJob(@PathVariable jobId: String): ResponseEntity<Any> {
        val job = scrapeJobService.getJob(jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/{tmdbId}/download/stream")
    fun downloadStream(
        @PathVariable tmdbId: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ): ResponseEntity<StreamingResponseBody> {
        val info = scrapeService.scrape(tmdbId, type, season, episode)
        return proxyStream(info.mediafire.directUrl, info.mediafire.filename ?: "${info.lamovieSlug}.rar")
    }

    @GetMapping("/slug/{slug}/download/stream")
    fun downloadBySlugStream(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) episode: Int?
    ): ResponseEntity<StreamingResponseBody> {
        val info = scrapeService.scrapeBySlug(slug, type, season, episode)
        return proxyStream(info.mediafire.directUrl, info.mediafire.filename ?: "${info.lamovieSlug}.rar")
    }

    @GetMapping("/mediafire/resolve")
    fun resolveMediafire(@RequestParam url: String) = scrapeService.resolveMediafire(url)

    @GetMapping("/mediafire/download")
    fun downloadMediafire(@RequestParam url: String): ResponseEntity<StreamingResponseBody> {
        val resolved = scrapeService.resolveMediafire(url)
        return proxyStream(resolved.directUrl, resolved.filename ?: "download")
    }

    @GetMapping("/mediafire/download-to-server")
    fun downloadMediafireToServer(@RequestParam url: String) = scrapeService.downloadMediafireToServer(url)

    @PostMapping("/mediafire/download-to-server")
    fun downloadMediafireToServerPost(@RequestParam url: String) = scrapeService.downloadMediafireToServer(url)

    private fun proxyStream(directUrl: String, filename: String): ResponseEntity<StreamingResponseBody> {
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build()
        val headReq = HttpRequest.newBuilder(URI(directUrl)).method("HEAD", HttpRequest.BodyPublishers.noBody())
            .header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(15)).build()
        var contentType: String? = null
        var contentLength: Long? = null
        try {
            val headRes = client.send(headReq, HttpResponse.BodyHandlers.discarding())
            contentType = headRes.headers().firstValue("content-type").orElse(null)
            contentLength = headRes.headers().firstValue("content-length").orElse(null)?.toLongOrNull()
        } catch (_: Exception) {}
        val stream = StreamingResponseBody { out ->
            val getReq = HttpRequest.newBuilder(URI(directUrl)).GET()
                .header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(120)).build()
            val res = client.send(getReq, HttpResponse.BodyHandlers.ofInputStream())
            res.body().transferTo(out)
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType(contentType ?: "application/octet-stream"))
            .apply { if (contentLength != null) header(HttpHeaders.CONTENT_LENGTH, contentLength.toString()) }
            .body(stream)
    }
}

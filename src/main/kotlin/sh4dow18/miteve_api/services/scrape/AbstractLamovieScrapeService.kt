package sh4dow18.miteve_api.services.scrape

import sh4dow18.miteve_api.dtos.scrape.MediafireResolveResponse
import sh4dow18.miteve_api.dtos.scrape.ScrapeDownloadResponse
import sh4dow18.miteve_api.dtos.scrape.ScrapeResponse

interface AbstractLamovieScrapeService {
    fun scrape(tmdbId: String, type: String, season: Int?, episode: Int?): ScrapeResponse
    fun scrapeBySlug(slug: String, type: String, season: Int?, episode: Int?): ScrapeResponse
    fun resolveMediafire(mediafireUrl: String): MediafireResolveResponse
    fun getDirectDownloadStream(mediafireUrl: String): Pair<String, java.io.InputStream>
    fun downloadToServer(tmdbId: String, type: String, season: Int?, episode: Int?): ScrapeDownloadResponse
    fun downloadBySlugToServer(slug: String, type: String, season: Int?, episode: Int?): ScrapeDownloadResponse
    fun downloadMediafireToServer(mediafireUrl: String): ScrapeDownloadResponse
}

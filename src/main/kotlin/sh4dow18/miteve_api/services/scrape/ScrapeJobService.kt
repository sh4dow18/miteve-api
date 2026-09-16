package sh4dow18.miteve_api.services.scrape

import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.scrape.ScrapeJobResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Service
class ScrapeJobService(
    private val scrapeService: LamovieScrapeService
) {

    private val jobs = ConcurrentHashMap<String, ScrapeJobResponse>()
    private val executor = Executors.newCachedThreadPool()

    fun startTorrentJob(tmdbId: String, type: String, season: Int?, episode: Int?): ScrapeJobResponse {
        return startDownloadJob(tmdbId, type, season, episode, "Torrent 1080p iniciado (fallback mediafire 404)")
    }

    fun startDownloadJob(tmdbId: String, type: String, season: Int?, episode: Int?, message: String = "Descarga iniciada"): ScrapeJobResponse {
        val jobId = UUID.randomUUID().toString()
        val initial = ScrapeJobResponse(jobId, "RUNNING", tmdbId, type, message = message)
        jobs[jobId] = initial
        executor.submit {
            try {
                val result = scrapeService.downloadToServer(tmdbId, type, season, episode)
                jobs[jobId] = ScrapeJobResponse(jobId, "COMPLETED", tmdbId, type, filename = result.filename, filePath = result.filePath, message = result.message)
            } catch (e: Exception) {
                jobs[jobId] = ScrapeJobResponse(jobId, "FAILED", tmdbId, type, error = e.message, message = e.message)
            }
        }
        return initial
    }

    fun getJob(jobId: String): ScrapeJobResponse? = jobs[jobId]
}

package sh4dow18.miteve_api.dtos.scrape

data class ScrapeJobResponse(
    val jobId: String,
    val status: String,
    val tmdbId: String,
    val type: String,
    val filename: String? = null,
    val filePath: String? = null,
    val message: String? = null,
    val error: String? = null
)

package sh4dow18.miteve_api.dtos.scrape

data class MediafireDownloadInfo(
    val mediafireUrl: String,
    val directUrl: String,
    val filename: String?,
    val quality: String?,
    val lang: String?,
    val size: String?
)

data class ScrapeResponse(
    val tmdbId: String,
    val type: String,
    val tmdbTitle: String?,
    val tmdbYear: String?,
    val lamovieId: Long,
    val lamovieSlug: String,
    val lamovieTitle: String,
    val lamovieUrl: String,
    val lamovieType: String,
    val season: Int?,
    val episode: Int?,
    val episodeId: Long?,
    val mediafire: MediafireDownloadInfo,
    val allDownloads: List<Map<String, Any?>>?,
    // Provider tracking for latino fallback chain (lamovie -> jkanime -> monoschinos -> animeflv -> consumet)
    val source: String = "lamovie",
    val sourceUrl: String? = null,
    val fallbackUsed: Boolean = false,
    val fallbackReason: String? = null,
    val attemptedProviders: List<String>? = null
)

data class MediafireResolveResponse(
    val mediafireUrl: String,
    val directUrl: String,
    val filename: String?
)

data class ScrapeDownloadResponse(
    val success: Boolean,
    val message: String,
    val tmdbId: String,
    val type: String,
    val lamovieUrl: String,
    val lamovieTitle: String,
    val mediafireUrl: String,
    val directUrl: String,
    val filename: String,
    val filePath: String,
    val fileSize: Long?,
    val season: Int? = null,
    val episode: Int? = null,
    val source: String = "lamovie",
    val fallbackUsed: Boolean = false
)

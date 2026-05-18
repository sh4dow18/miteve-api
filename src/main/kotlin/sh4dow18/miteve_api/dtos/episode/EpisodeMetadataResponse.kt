package sh4dow18.miteve_api.dtos.episode

data class EpisodeMetadataResponse(
    var id: String,
    var title: String,
    var episodeNumber: Int,
    var beginSummary: Long?,
    var endSummary: Long?,
    var beginIntro: Long?,
    var endIntro: Long?,
    var beginCredits: Long?,
)

package sh4dow18.miteve_api.dtos.episode

data class FullEpisodeResponse(
    var id: String,
    var episodeNumber: Int,
    var title: String,
    var cover: String,
    var description: String,
    var beginSummary: Long?,
    var endSummary: Long?,
    var beginIntro: Long?,
    var endIntro: Long?,
    var beginCredits: Long?,
)

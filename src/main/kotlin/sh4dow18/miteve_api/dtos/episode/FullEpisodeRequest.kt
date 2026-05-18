package sh4dow18.miteve_api.dtos.episode

data class FullEpisodeRequest(
    var episodeNumber: Int,
    var title: String,
    var description: String,
    var beginSummary: Long?,
    var endSummary: Long?,
    var beginIntro: Long?,
    var endIntro: Long?,
    var beginCredits: Long?,
)


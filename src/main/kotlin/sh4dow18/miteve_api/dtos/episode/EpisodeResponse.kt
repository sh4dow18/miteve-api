package sh4dow18.miteve_api.dtos.episode

data class EpisodeResponse(
    var id: String,
    var episodeNumber: Int,
    var title: String,
    var description: String?,
    var cover: String,
)
package sh4dow18.miteve_api.dtos.continue_watching

import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.episode.EpisodeMetadataResponse

data class ContinueWatchingResponse(
    var id: Long,
    var time: Long,
    var content: MiniContentResponse?,
    var episode: EpisodeMetadataResponse?,
)


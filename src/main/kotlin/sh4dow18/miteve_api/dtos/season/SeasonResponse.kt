package sh4dow18.miteve_api.dtos.season

import sh4dow18.miteve_api.dtos.episode.EpisodeResponse

data class SeasonResponse(
    var id: String,
    var seasonNumber: Int,
    var episodesList: List<EpisodeResponse>
)
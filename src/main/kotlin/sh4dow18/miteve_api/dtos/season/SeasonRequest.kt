package sh4dow18.miteve_api.dtos.season

import sh4dow18.miteve_api.dtos.episode.EpisodeRequest

data class SeasonRequest(
    var seasonNumber: Int,
    var comingSoon: Boolean,
    var episodesList: List<EpisodeRequest>
)

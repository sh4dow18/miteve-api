package sh4dow18.miteve_api.dtos.batch_update

data class BatchUpdateEpisodesRequest(
    val seriesId: String,
    val startSeason: Int,
    val startEpisode: Int,
    val endSeason: Int,
    val endEpisode: Int,
    val endingDuration: Long
)

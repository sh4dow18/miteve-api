package sh4dow18.miteve_api.dtos.batch_update

data class BatchUpdateEpisodesResponse(
    val seriesId: String,
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
)

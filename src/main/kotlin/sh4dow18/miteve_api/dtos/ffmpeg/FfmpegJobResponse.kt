package sh4dow18.miteve_api.dtos.ffmpeg

data class FfmpegJobResponse(
    val jobId: String,
    val status: String,
    val filename: String,
    val type: String,
    val message: String? = null,
    val outputFiles: List<String> = emptyList(),
    val commandOutputs: List<String> = emptyList(),
    val error: String? = null
)

package sh4dow18.miteve_api.dtos.ffmpeg

data class FfmpegResponse(
    val success: Boolean,
    val message: String,
    val filename: String,
    val commandOutputs: List<String>,
    val outputFiles: List<String>
)

package sh4dow18.miteve_api.dtos.ffmpeg

data class FfmpegRequest(
    val filename: String,
    val audioTrack: Int? = null,
    val subtitleTrack: Int? = null
)

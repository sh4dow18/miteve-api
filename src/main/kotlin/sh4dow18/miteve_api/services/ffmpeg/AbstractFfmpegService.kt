package sh4dow18.miteve_api.services.ffmpeg

import sh4dow18.miteve_api.dtos.ffmpeg.FfmpegResponse

interface AbstractFfmpegService {
    fun transformTo1080p(filename: String, audioTrack: Int? = null, subtitleTrack: Int? = null): FfmpegResponse
    fun transformTo360p(filename: String): FfmpegResponse
    fun transformMkvsToHlsDash(filename: String): FfmpegResponse
    fun unrar(filename: String): FfmpegResponse
}

package sh4dow18.miteve_api.services.ffmpeg

import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.ffmpeg.FfmpegJobResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Service
class FfmpegJobService(
    private val ffmpegService: AbstractFfmpegService
) {

    private val jobs = ConcurrentHashMap<String, FfmpegJobResponse>()
    private val executor = Executors.newCachedThreadPool()

    fun startJob(filename: String, type: String, audioTrack: Int? = null, subtitleTrack: Int? = null): FfmpegJobResponse {
        val jobId = UUID.randomUUID().toString()
        val initial = FfmpegJobResponse(jobId, "RUNNING", filename, type, message = "Iniciado a:$audioTrack s:$subtitleTrack")
        jobs[jobId] = initial
        executor.submit {
            try {
                val result = when (type) {
                    "1080p" -> ffmpegService.transformTo1080p(filename, audioTrack, subtitleTrack)
                    "360p" -> ffmpegService.transformTo360p(filename)
                    "dash" -> ffmpegService.transformMkvsToHlsDash(filename)
                    else -> throw IllegalArgumentException("tipo $type desconocido")
                }
                jobs[jobId] = FfmpegJobResponse(jobId, "COMPLETED", filename, type, message = result.message, outputFiles = result.outputFiles, commandOutputs = result.commandOutputs)
            } catch (e: Exception) {
                jobs[jobId] = FfmpegJobResponse(jobId, "FAILED", filename, type, error = e.message, message = e.message)
            }
        }
        return initial
    }

    fun getJob(jobId: String): FfmpegJobResponse? = jobs[jobId]

    fun listJobs(): List<FfmpegJobResponse> = jobs.values.toList()
}

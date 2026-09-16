package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.ffmpeg.FfmpegRequest
import sh4dow18.miteve_api.services.ffmpeg.AbstractFfmpegService
import sh4dow18.miteve_api.services.ffmpeg.FfmpegJobService

@RestController
@RequestMapping("\${endpoint.ffmpeg}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class FfmpegController(
    private val ffmpegService: AbstractFfmpegService,
    private val jobService: FfmpegJobService
) {

    @PostMapping("/1080p", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun transformTo1080p(@RequestBody req: FfmpegRequest, @RequestParam(defaultValue = "false") async: Boolean): ResponseEntity<Any> {
        return if (async) ResponseEntity.accepted().body(jobService.startJob(req.filename, "1080p", req.audioTrack, req.subtitleTrack))
        else ResponseEntity.ok(ffmpegService.transformTo1080p(req.filename, req.audioTrack, req.subtitleTrack))
    }

    @PostMapping("/360p", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun transformTo360p(@RequestBody req: FfmpegRequest, @RequestParam(defaultValue = "false") async: Boolean): ResponseEntity<Any> {
        return if (async) ResponseEntity.accepted().body(jobService.startJob(req.filename, "360p"))
        else ResponseEntity.ok(ffmpegService.transformTo360p(req.filename))
    }

    @PostMapping("/dash", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun transformMkvsToHlsDash(@RequestBody req: FfmpegRequest, @RequestParam(defaultValue = "false") async: Boolean): ResponseEntity<Any> {
        return if (async) ResponseEntity.accepted().body(jobService.startJob(req.filename, "dash"))
        else ResponseEntity.ok(ffmpegService.transformMkvsToHlsDash(req.filename))
    }

    @GetMapping("/job/{jobId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getJob(@PathVariable jobId: String): ResponseEntity<Any> {
        val job = jobService.getJob(jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/jobs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listJobs() = jobService.listJobs()
}

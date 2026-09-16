package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.upload.UploadRequest
import sh4dow18.miteve_api.services.upload.AbstractUploadService

@RestController
@RequestMapping("\${endpoint.upload}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class UploadController(
    private val uploadService: AbstractUploadService
) {

    @PostMapping("/to-server", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadToServer(@RequestBody req: UploadRequest) = uploadService.uploadToServer(req.filename)

    @PostMapping("/mkdir", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createFolder(@RequestBody req: UploadRequest) = uploadService.createFolder(req.filename)

    @PostMapping("/folder", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createFolderAlias(@RequestBody req: UploadRequest) = uploadService.createFolder(req.filename)

    @GetMapping("/verify", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun verifyPath(@RequestParam path: String) = uploadService.verifyPath(path)

    @GetMapping("/verify-dash", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun verifyDash(
        @RequestParam slug: String,
        @RequestParam(defaultValue = "movie") type: String,
        @RequestParam(required = false) tmdbId: Long?
    ) = uploadService.verifyDash(slug, type, tmdbId)
}

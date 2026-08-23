package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportRequest
import sh4dow18.miteve_api.dtos.suggested_content_report.UpdateSuggestedContentReportStatusRequest
import sh4dow18.miteve_api.services.suggested_content_report.SuggestedContentReportService

@RestController
@RequestMapping("\${endpoint.suggested-content-reports}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class SuggestedContentReportController(private val service: SuggestedContentReportService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAll() = service.findAll()
    @GetMapping("{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findById(@PathVariable id: Long) = service.findById(id)
    @GetMapping("user/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAllByUserId(@PathVariable userId: Long) = service.findAllByUserId(userId)
    @GetMapping("exists/{tmdbId}/{contentTypeId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun existsByTmdbIdAndContentTypeId(@PathVariable tmdbId: Long, @PathVariable contentTypeId: Long) = service.existsByTmdbIdAndContentTypeId(tmdbId, contentTypeId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody request: SuggestedContentReportRequest) = service.insert(request)
    @PatchMapping("{id}/status", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun updateStatus(@PathVariable id: Long, @RequestBody request: UpdateSuggestedContentReportStatusRequest) = service.updateStatus(id, request)
}

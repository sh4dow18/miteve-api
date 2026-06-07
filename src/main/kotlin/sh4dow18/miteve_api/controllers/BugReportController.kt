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
import sh4dow18.miteve_api.dtos.bug_report.BugReportRequest
import sh4dow18.miteve_api.dtos.bug_report.UpdateBugReportStatusRequest
import sh4dow18.miteve_api.services.bug_report.BugReportService

// Bug Report Rest Controller
@RestController
@RequestMapping("\${endpoint.bug-reports}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class BugReportController(private val bugReportService: BugReportService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAll() = bugReportService.findAll()
    @GetMapping("{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findById(@PathVariable id: Long) = bugReportService.findById(id)
    @GetMapping("user/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAllByUserId(@PathVariable userId: Long) = bugReportService.findAllByUserId(userId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody request: BugReportRequest) = bugReportService.insert(request)
    @PatchMapping("{id}/status", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun updateStatus(@PathVariable id: Long, @RequestBody request: UpdateBugReportStatusRequest) = bugReportService.updateStatus(id, request)
}

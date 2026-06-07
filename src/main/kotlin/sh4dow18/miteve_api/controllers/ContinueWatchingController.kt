package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import sh4dow18.miteve_api.dtos.continue_watching.AddContinueWatchingRequest
import sh4dow18.miteve_api.dtos.continue_watching.UpdateContinueWatchingTimeRequest
import sh4dow18.miteve_api.services.continue_watching.ContinueWatchingService

// ContinueWatching Rest Controller
@RestController
@RequestMapping("\${endpoint.continue-watching}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ContinueWatchingController(private val continueWatchingService: ContinueWatchingService) {
    @PutMapping("{id}/time", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun updateTime(
        @PathVariable id: Long,
        @RequestBody request: UpdateContinueWatchingTimeRequest
    ) = continueWatchingService.updateTime(id, request)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun addOrUpdate(@RequestBody request: AddContinueWatchingRequest) = continueWatchingService.addOrUpdate(request)
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = continueWatchingService.delete(id)
}


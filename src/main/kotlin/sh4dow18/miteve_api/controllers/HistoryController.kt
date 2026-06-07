package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.history.HistoryService

// History Rest Controller
@RestController
@RequestMapping("\${endpoint.history}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class HistoryController(private val historyService: HistoryService) {
    @GetMapping("profile/{profileId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAllByProfileId(@PathVariable profileId: Long) = historyService.findAllByProfileId(profileId)
}

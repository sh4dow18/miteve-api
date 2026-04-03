package sh4dow18.miteve_api.controllers

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.season.SeasonService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.seasons}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class SeasonController(private val seasonService: SeasonService) {
    @GetMapping("{id}/episodes")
    @ResponseBody
    fun findEpisodesById(@PathVariable id: String) = seasonService.findEpisodesById(id)
}
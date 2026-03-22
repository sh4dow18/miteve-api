package sh4dow18.miteve_api.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.episode.EpisodeService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.episodes}")
class EpisodeController(private val episodeService: EpisodeService) {
    @GetMapping("next/{id}")
    @ResponseBody
    fun findNextById(@PathVariable id: String) = episodeService.findNextById(id)
    @GetMapping("metadata/{id}")
    @ResponseBody
    fun findMetadataById(@PathVariable id: String) = episodeService.findMetadataById(id)
}
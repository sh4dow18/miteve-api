package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.episode.FullEpisodeRequest
import sh4dow18.miteve_api.services.episode.EpisodeService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.episodes}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class EpisodeController(private val episodeService: EpisodeService) {
    @GetMapping("next/{id}")
    @ResponseBody
    fun findNextById(@PathVariable id: String) = episodeService.findNextById(id)
    @GetMapping("metadata/{id}")
    @ResponseBody
    fun findMetadataById(@PathVariable id: String) = episodeService.findMetadataById(id)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: String, @RequestBody fullEpisodeRequest: FullEpisodeRequest) = episodeService.update(id, fullEpisodeRequest)
}
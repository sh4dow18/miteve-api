package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.content.ContentRequest
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.season.SeasonRequest
import sh4dow18.miteve_api.services.content.ContentService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.contents}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ContentController(private val contentService: ContentService) {
    @GetMapping
    @ResponseBody
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = contentService.findAll(page, size)
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: String) = contentService.findById(id)
    @GetMapping("recent")
    @ResponseBody
    fun findRecentContent() = contentService.findRecentContent()
    @GetMapping("soon")
    @ResponseBody
    fun findComingSoon() = contentService.findComingSoon()
    @GetMapping("{id}/seasons")
    @ResponseBody
    fun findSeasonsById(@PathVariable id: String) = contentService.findSeasonsById(id)
    @GetMapping("like/{title}")
    fun findByTitle(@PathVariable title: String) = contentService.findByTitle(title)
    @GetMapping("search")
    @ResponseBody
    fun searchByTitle(
        @RequestParam title: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = contentService.searchByTitle(title, page, size)
    @GetMapping("genre/{genreId}")
    @ResponseBody
    fun findByGenre(
        @PathVariable genreId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ) = contentService.findByGenre(genreId, page, size)
    @GetMapping("{id}/similar")
    @ResponseBody
    fun findSimilarContent(
        @PathVariable id: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "15") size: Int
    ) = contentService.findSimilarContent(id, page, size)
    @GetMapping("recommendations/{profileId}")
    @ResponseBody
    fun findRecommendedContent(@PathVariable profileId: Long) = contentService.findRecommendedContent(profileId)
    @GetMapping("watch-again/{profileId}")
    @ResponseBody
    fun findWatchAgain(@PathVariable profileId: Long) = contentService.findWatchAgain(profileId)
    @GetMapping("top-watched")
    @ResponseBody
    fun findTopWatched() = contentService.findTopWatched()
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody contentRequest: ContentRequest) = contentService.insert(contentRequest)
    @PostMapping("{id}/seasons", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insertEpisodes(@PathVariable id: String, @RequestBody seasonsList: List<SeasonRequest>) = contentService.insertEpisodes(id, seasonsList)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: String, @RequestBody contentRequest: ContentRequest) = contentService.update(id, contentRequest)
}
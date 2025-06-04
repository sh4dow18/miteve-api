package sh4dow18.miteve_api
// Rest Controllers Requirements
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
// Genre Rest Controller
@RestController
@RequestMapping("\${endpoint.genres}")
@CrossOrigin(origins = ["http://localhost:3001"])
class GenreRestController(private val genreService: GenreService) {
    @GetMapping
    @ResponseBody
    fun findAll() = genreService.findAll()
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody genreRequest: GenreRequest): GenreResponse {
        return genreService.insert(genreRequest)
    }
}
// Movie Rest Controller
@RestController
@RequestMapping("\${endpoint.movies}")
@CrossOrigin(origins = ["http://localhost:3001"])
class MovieRestController(private val movieService: MovieService) {
    @GetMapping
    @ResponseBody
    fun findAll() = movieService.findAll()
    @GetMapping("recommendations/{id}")
    @ResponseBody
    fun findAllRecommendationsById(@PathVariable id: Long) = movieService.findAllRecommendationsById(id)
    @GetMapping("minimal/{id}")
    @ResponseBody
    fun findByIdMinimal(@PathVariable id: Long) = movieService.findByIdMinimal(id)
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: Long) = movieService.findById(id)
    @GetMapping("stream/{id}")
    @ResponseBody
    fun streamMovie(@PathVariable id: Long, @RequestParam("quality") quality: String?, request: HttpServletRequest, response: HttpServletResponse) =
        movieService.streamMovie(id, request.getHeader("Range"), quality, response)
    @GetMapping("subtitles/{id}")
    @ResponseBody
    fun streamSubtitles(@PathVariable id: Long, response: HttpServletResponse) = movieService.streamSubtitles(id, response)
    @RequestMapping("stream/{id}", method = [RequestMethod.HEAD])
    fun streamMovieHead(@PathVariable id: Long, @RequestParam quality: String?): ResponseEntity<Void> = movieService.streamMovieHead(id, quality)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody movieRequest: MovieRequest) = movieService.insert(movieRequest)
}
// Series Rest Controller
@RestController
@RequestMapping("\${endpoint.series}")
@CrossOrigin(origins = ["http://localhost:3001"])
class SeriesRestController(private val seriesService: SeriesService) {
    @GetMapping
    @ResponseBody
    fun findAll() = seriesService.findAll()
    @GetMapping("recommendations/{id}")
    @ResponseBody
    fun findAllRecommendationsById(@PathVariable id: Long) = seriesService.findAllRecommendationsById(id)
    @GetMapping("minimal/{id}")
    @ResponseBody
    fun findByIdMinimal(@PathVariable id: Long) = seriesService.findByIdMinimal(id)
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: Long) = seriesService.findById(id)
    @GetMapping("{id}/season/{seasonNumber}")
    @ResponseBody
    fun findSeasonByNumber(@PathVariable id: Long, @PathVariable seasonNumber: Int) = seriesService.findSeasonByNumber(id, seasonNumber)
    @GetMapping("stream/{id}")
    @ResponseBody
    fun streamEpisode(@PathVariable id: Long, @RequestParam("quality") quality: String?, request: HttpServletRequest, response: HttpServletResponse) =
        seriesService.streamEpisode(id, request.getHeader("Range"), quality, response)
    @GetMapping("subtitles/{id}")
    @ResponseBody
    fun streamSubtitles(@PathVariable id: Long, response: HttpServletResponse) = seriesService.streamSubtitles(id, response)
    @RequestMapping("stream/{id}", method = [RequestMethod.HEAD])
    fun streamEpisodeHead(@PathVariable id: Long, @RequestParam quality: String?): ResponseEntity<Void> = seriesService.streamEpisodeHead(id, quality)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody seriesRequest: SeriesRequest) = seriesService.insert(seriesRequest)
    @PostMapping("{id}/episodes", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insertEpisodes(@PathVariable id: Long, @RequestBody seasonsList: List<SeasonRequest>) = seriesService.insertEpisodes(id, seasonsList)
}
package sh4dow18.miteve_api
// Rest Controllers Requirements
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody movieRequest: MovieRequest): MovieResponse {
        return movieService.insert(movieRequest)
    }
}
package sh4dow18.miteve_api
/* Main Comments

* @Transactional is a Tag that establishes that is a Transactional Service Function. This one makes a transaction
* when this service function is in operation.

* */
// Services Requirements
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
// Genre Service Interface where the functions to be used in
// Spring Abstract Genre Service are declared
interface GenreService {
    fun findAll(): List<GenreResponse>
    fun insert(genreRequest: GenreRequest): GenreResponse
}
// Spring Abstract Genre Service
@Service
class AbstractGenreService(
    // Genre Service Props
    @Autowired
    val genreRepository: GenreRepository,
    @Autowired
    val genreMapper: GenreMapper
): GenreService {
    override fun findAll(): List<GenreResponse> {
        // Returns all Genres as a Genres Responses List
        return genreMapper.genresListToGenreResponsesList(genreRepository.findAll())
    }
    override fun insert(genreRequest: GenreRequest): GenreResponse {
        val existingGenre = genreRepository.findByNameIgnoreCase(genreRequest.name).orElse(null)
        if (existingGenre != null) {
            throw ElementAlreadyExists(existingGenre.name, "Genre")
        }
        // Create the new genre
        val newGenre = genreMapper.genreRequestToGenre(genreRequest)
        // Save new Genre and Returns it as Genre Response
        return genreMapper.genreToGenreResponse(genreRepository.save(newGenre))
    }
}
// Movie Service Interface where the functions to be used in
// Spring Abstract Movie Service are declared
interface MovieService {
    fun findAll(): List<MinimalMovieResponse>
    fun insert(movieRequest: MovieRequest): MovieResponse
}
// Spring Abstract Movie Service
@Service
class AbstractMovieService(
    // Movie Service Props
    @Autowired
    val movieRepository: MovieRepository,
    @Autowired
    val movieMapper: MovieMapper,
    @Autowired
    val genreRepository: GenreRepository,
): MovieService {
    override fun findAll(): List<MinimalMovieResponse> {
        // Returns all Movies as a Movies Responses List
        return movieMapper.moviesListToMovieResponsesList(movieRepository.findAll())
    }
    override fun insert(movieRequest: MovieRequest): MovieResponse {
        // If the Movie Database Id is null, do the following
        if (movieRequest.tmdbId != null) {
            // Check if the movie already exists with the same TMDB Id
            if (movieRepository.findByTmdbId(movieRequest.tmdbId!!).orElse(null) != null) {
                throw ElementAlreadyExists("${movieRequest.tmdbId}", "Movie")
            }
        }
        // Check if each genre submitted exists
        val genresList = genreRepository.findAllById(movieRequest.genresList)
        if (genresList.size != movieRequest.genresList.size) {
            val missingIds = movieRequest.genresList - genresList.map { it.id }.toSet()
            throw NoSuchElementExists(missingIds.toList().toString(), "Genres")
        }
        // If the movie not exists and each genre exists, create the new movie
        val newMovie = movieMapper.movieRequestToMovie(movieRequest, genresList.toSet())
        // Transforms the New movie to a movie Response and Returns it
        return movieMapper.movieToMovieResponse(movieRepository.save(newMovie))
    }

}
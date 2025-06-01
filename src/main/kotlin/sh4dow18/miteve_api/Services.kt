package sh4dow18.miteve_api
/* Main Comments

* @Transactional is a Tag that establishes that is a Transactional Service Function. This one makes a transaction
* when this service function is in operation.

* */
// Services Requirements
import jakarta.servlet.http.HttpServletResponse
import org.apache.catalina.connector.ClientAbortException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import java.io.RandomAccessFile
import java.nio.file.Paths

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
    fun findAllRecommendationsById(id: Long): List<MinimalMovieResponse>
    fun findByIdMinimal(id: Long): MinimalMovieResponse
    fun findById(id: Long): MovieResponse
    fun insert(movieRequest: MovieRequest): MovieResponse
    fun streamMovie(id: Long, rangeHeader: String?, response: HttpServletResponse)
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
    @Value("\${video_path}")
    val videoPath: String? = null
): MovieService {
    override fun findAll(): List<MinimalMovieResponse> {
        // Returns all Movies as a Movies Responses List
        return movieMapper.moviesListToMinimalMovieResponsesList(movieRepository.findAll())
    }
    override fun findAllRecommendationsById(id: Long): List<MinimalMovieResponse> {
        val movie = movieRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Movie")
        }
        val recommendationsList = movieRepository.findByCollectionAndIdNot(movie.collection, movie.id) +
                movieRepository.findTop10ByGenresListIdAndCollectionNot(movie.genresList.toList()[0].id, movie.collection)
        return movieMapper.moviesListToMinimalMovieResponsesList(recommendationsList)
    }
    override fun findByIdMinimal(id: Long): MinimalMovieResponse {
        val movie = movieRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Movie")
        }
        return movieMapper.movieToMinimalMovieResponse(movie)
    }
    override fun findById(id: Long): MovieResponse {
        val movie = movieRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Movie")
        }
        return movieMapper.movieToMovieResponse(movie)
    }
    override fun insert(movieRequest: MovieRequest): MovieResponse {
        // Check if the movie already exists with the same TMDB Id
        if (movieRepository.findById(movieRequest.id).orElse(null) != null) {
            throw ElementAlreadyExists("${movieRequest.id}", "Movie")
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
    override fun streamMovie(id: Long, rangeHeader: String?, response: HttpServletResponse) {
        try {
            // Define the path to the directory where the videos are stored
            val videoPath = Paths.get("$videoPath/movies/")
            // Get the actual video file based on the id provided
            val videoFile = videoPath.resolve("$id.webm").toFile()
            // If the video file does not exist, return Not Found
            if (!videoFile.exists()) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                return
            }
            // Get the total size of the video file in bytes
            val fileLength = videoFile.length()
            // Open the file for random access reading to support byte-range requests
            val inputFile = RandomAccessFile(videoFile, "r")
            try {
                // If there is no Range header, or it does not start with "bytes=", send the whole file
                if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                    response.status = HttpServletResponse.SC_OK
                    response.contentType = "video/webm"
                    response.setHeader("Content-Length", fileLength.toString())
                    // Stream the entire video file to the response output stream
                    videoFile.inputStream().use { input ->
                        response.outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    return
                }
                // If there is a Range header, parse it to extract the byte range
                val matcher = Regex("bytes=(\\d+)-(\\d*)").find(rangeHeader)
                if (matcher != null) {
                    val startStr = matcher.groups[1]?.value
                    val endStr = matcher.groups[2]?.value
                    // Convert the start and end of the range to Long values
                    val start = startStr?.toLongOrNull() ?: 0
                    val end = if (!endStr.isNullOrEmpty()) endStr.toLong() else fileLength - 1
                    // Validate the range: start must be <= end, and end must be within the file size
                    if (start > end || end >= fileLength) {
                        response.status = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE
                        response.setHeader("Content-Range", "bytes */$fileLength")
                        return
                    }
                    // Calculate how many bytes it will send
                    val contentLength = end - start + 1
                    response.status = HttpServletResponse.SC_PARTIAL_CONTENT
                    response.contentType = "video/webm"
                    response.setHeader("Accept-Ranges", "bytes")
                    response.setHeader("Content-Length", contentLength.toString())
                    response.setHeader("Content-Range", "bytes $start-$end/$fileLength")
                    // Move the file pointer to the start of the requested range
                    inputFile.seek(start)
                    val buffer = ByteArray(8192)
                    var bytesToRead = contentLength
                    val output = response.outputStream
                    // Read from the file and write to the response in chunks until all bytes are sent
                    while (bytesToRead > 0) {
                        val read = inputFile.read(buffer, 0, minOf(buffer.size.toLong(), bytesToRead).toInt())
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesToRead -= read
                    }
                } else {
                    // If the Range header is malformed, return 400 Bad Request
                    response.status = HttpServletResponse.SC_BAD_REQUEST
                }
            }
            catch (_: ClientAbortException) {}
            catch (_: AsyncRequestNotUsableException) {}
            finally {
                // Close the RandomAccessFile
                inputFile.close()
            }
        }
        catch (_: ClientAbortException) {}
        catch (e: Exception) {
            // If any unexpected error occurs, print stack trace and return Internal Server Error
            e.printStackTrace()
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }
    }
}
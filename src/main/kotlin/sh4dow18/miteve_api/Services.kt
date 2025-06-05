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
import org.springframework.http.ResponseEntity
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
    fun streamMovieHead(id: Long, quality: String?): ResponseEntity<Void>
    fun streamMovie(id: Long, rangeHeader: String?, quality: String?, response: HttpServletResponse)
    fun streamSubtitles(id: Long, response: HttpServletResponse)
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
    override fun streamMovieHead(id: Long, quality: String?): ResponseEntity<Void> {
        // Define the path to the directory where the movies are stored
        val moviesPath = Paths.get("$videoPath/movies/")
        val filename = if (quality == "low") "$id-low.webm" else "$id.webm"
        val videoFile = moviesPath.resolve(filename).toFile()
        // If the video file does not exist, return Not Found
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok().build()
    }
    override fun streamMovie(id: Long, rangeHeader: String?, quality: String?, response: HttpServletResponse) {
        try {
            val originalFile = "$id.webm"
            // Define the path to the directory where the movies are stored
            val moviesPath = Paths.get("$videoPath/movies/")
            var filename = if (quality == "low") "$id-low.webm" else originalFile
            // Get the actual video file based on the id provided
            var videoFile = moviesPath.resolve(filename).toFile()
            // If the video file does not exist, return Not Found
            if (!videoFile.exists()) {
                if (quality != "low") {
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    return
                }
                filename = originalFile
                videoFile = moviesPath.resolve(filename).toFile()
                if (!videoFile.exists()) {
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    return
                }
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
    override fun streamSubtitles(id: Long, response: HttpServletResponse) {
        // Define the path to the directory where the movies are stored
        val moviesPath = Paths.get("$videoPath/movies/")
        // Get the actual video file based on the id provided
        val videoFile = moviesPath.resolve("$id.webm").toFile()
        // If the video file does not exist, return Not Found
        if (!videoFile.exists()) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }
        // Prepare Ffmpeg to get the subtitles from video
        val pb = ProcessBuilder(
            "ffmpeg",
            "-i", videoFile.absolutePath,
            "-map", "0:s:0",
            "-f", "webvtt",
            "-"
        )
        var process: Process? = null
        try {
            // Execute the command to get subtitles
            process = pb.start()
            // Get Subtitles as a variable
            val processOutput = process.inputStream
            // Check if the video has subtitles
            val checkBuffer = ByteArray(1024)
            val bytesRead = processOutput.read(checkBuffer)
            if (bytesRead == -1) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                process.destroy()
                return
            }
            // Add WebVtt content type
            response.contentType = "text/vtt"
            response.outputStream.write(checkBuffer, 0, bytesRead)
            // Put al WebVtt in HTTP Response
            val buffer = ByteArray(8192)
            var len: Int
            while (processOutput.read(buffer).also { len = it } != -1) {
                response.outputStream.write(buffer, 0, len)
            }
            response.outputStream.flush()
            process.waitFor()

        } catch (e: Exception) {
            // If any unexpected error occurs, print stack trace and return Internal Server Error
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            process?.destroy()
        }
    }
}
// Series Service Interface where the functions to be used in
// Spring Abstract Series Service are declared
interface SeriesService {
    fun findAll(): List<MinimalSeriesResponse>
    fun findAllRecommendationsById(id: Long): List<MinimalSeriesResponse>
    fun findByIdMinimal(id: Long): MinimalSeriesResponse
    fun findById(id: Long): SeriesResponse
    fun findSeasonByNumber(id: Long, seasonNumber: Int): SeasonResponse
    fun insert(seriesRequest: SeriesRequest): SeriesResponse
    fun insertEpisodes(id: Long, seasonsList: List<SeasonRequest>): InsertEpisodesResponse
    fun streamEpisodeHead(id: Long, seasonNumber: Int, episodeNumber: Int, quality: String?): ResponseEntity<Void>
    fun streamEpisode(id: Long, seasonNumber: Int, episodeNumber: Int, rangeHeader: String?, quality: String?, response: HttpServletResponse)
    fun streamSubtitles(id: Long, seasonNumber: Int, episodeNumber: Int, response: HttpServletResponse)
}
// Spring Abstract Movie Service
@Service
class AbstractSeriesService(
    // Movie Service Props
    @Autowired
    val seriesRepository: SeriesRepository,
    @Autowired
    val genreRepository: GenreRepository,
    @Autowired
    val seasonRepository: SeasonRepository,
    @Autowired
    val episodeRepository: EpisodeRepository,
    @Autowired
    val seriesMapper: SeriesMapper,
    @Autowired
    val seasonMapper: SeasonMapper,
    @Autowired
    val episodeMapper: EpisodeMapper,
    @Value("\${video_path}")
    val videoPath: String? = null
): SeriesService {
    override fun findAll(): List<MinimalSeriesResponse> {
        // Returns all Movies as a Movies Responses List
        return seriesMapper.seriesListToMinimalSeriesResponsesList(seriesRepository.findAll())
    }
    override fun findAllRecommendationsById(id: Long): List<MinimalSeriesResponse> {
        val series = seriesRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Series")
        }
        val recommendationsList = genreRepository.findSeriesByGenreNameIgnoreCase(series.genresList.toList()[0].name)
        return seriesMapper.seriesListToMinimalSeriesResponsesList(recommendationsList.toList())
    }
    override fun findByIdMinimal(id: Long): MinimalSeriesResponse {
        val series = seriesRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Series")
        }
        return seriesMapper.seriesToMinimalSeriesResponse(series)
    }
    override fun findById(id: Long): SeriesResponse {
        val series = seriesRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Series")
        }
        return seriesMapper.seriesToSeriesResponse(series)
    }
    override fun findSeasonByNumber(id: Long, seasonNumber: Int): SeasonResponse {
        val series = seriesRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Series")
        }
        val season = series.seasonsList.find { it.seasonNumber == seasonNumber }
        if (season == null) {
            throw NoSuchElementExists("$seasonNumber", "Season in Series with id $id")
        }
        return seasonMapper.seasonToSeasonResponse(season)
    }
    override fun insert(seriesRequest: SeriesRequest): SeriesResponse {
        // Check if the series already exists with the same TMDB Id
        if (seriesRepository.findById(seriesRequest.id).orElse(null) != null) {
            throw ElementAlreadyExists("${seriesRequest.id}", "Series")
        }
        // Check if each genre submitted exists
        val genresList = genreRepository.findAllById(seriesRequest.genresList)
        if (genresList.size != seriesRequest.genresList.size) {
            val missingIds = seriesRequest.genresList - genresList.map { it.id }.toSet()
            throw NoSuchElementExists(missingIds.toList().toString(), "Genres")
        }
        // If the series not exists and each genre exists, create the new series
        val newSeries = seriesMapper.seriesRequestToSeries(seriesRequest, genresList.toSet())
        // Transforms the New series to a series Response and Returns it
        return seriesMapper.seriesToSeriesResponse(seriesRepository.save(newSeries))
    }
    override fun insertEpisodes(id: Long, seasonsList: List<SeasonRequest>): InsertEpisodesResponse {
        // Check if the series already exists with the same TMDB Id
        val series = seriesRepository.findById(id).orElseThrow {
            NoSuchElementExists("$id", "Series")
        }
        seasonsList.forEach { seasonRequest ->
            // Check if exists already the season submitted
            val existingSeason = series.seasonsList.find { it.seasonNumber == seasonRequest.seasonNumber }
            // If the season exists, set the existing season, if not, create a new one
            val season = existingSeason ?: seasonRepository.save(seasonMapper.seasonRequestToSeason(seasonRequest, series))
            seasonRequest.episodesList.forEach { episodeRequest ->
                // Check if exists already the episode submitted from the season submitted
                val existingEpisode = season.episodesList.find { it.episodeNumber == episodeRequest.episodeNumber }
                // If the episode does not exist, create a new one
                if (existingEpisode == null) {
                    episodeRepository.save(episodeMapper.episodeRequestToEpisode(episodeRequest, season))
                }
            }
        }
        return InsertEpisodesResponse(id = series.id, seasonsList = series.seasonsList.map { it.seasonNumber })
    }
    override fun streamEpisodeHead(id: Long, seasonNumber: Int, episodeNumber: Int, quality: String?): ResponseEntity<Void> {
        // Define the path to the directory where the series are stored
        val seriesPath = Paths.get("$videoPath/series/$id/Temporada $seasonNumber/")
        val filename = if (quality == "low") "Episodio $episodeNumber-low.webm" else "Episodio $episodeNumber.webm"
        val videoFile = seriesPath.resolve(filename).toFile()
        // If the video file does not exist, return Not Found
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok().build()
    }
    override fun streamEpisode(id: Long, seasonNumber: Int, episodeNumber: Int, rangeHeader: String?, quality: String?, response: HttpServletResponse) {
        try {
            val originalFile = "Episodio $episodeNumber.webm"
            // Define the path to the directory where the movies are stored
            val moviesPath = Paths.get("$videoPath/series/$id/Temporada $seasonNumber/")
            var filename = if (quality == "low") "Episodio $episodeNumber-low.webm" else originalFile
            // Get the actual video file based on the id provided
            var videoFile = moviesPath.resolve(filename).toFile()
            // If the video file does not exist, return Not Found
            if (!videoFile.exists()) {
                if (quality != "low") {
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    return
                }
                filename = originalFile
                videoFile = moviesPath.resolve(filename).toFile()
                if (!videoFile.exists()) {
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    return
                }
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
    override fun streamSubtitles(id: Long, seasonNumber: Int, episodeNumber: Int, response: HttpServletResponse) {
        // Define the path to the directory where the movies are stored
        val moviesPath = Paths.get("$videoPath/series/$id/Temporada $seasonNumber/")
        // Get the actual video file based on the id provided
        val videoFile = moviesPath.resolve("Episodio $episodeNumber.webm").toFile()
        // If the video file does not exist, return Not Found
        if (!videoFile.exists()) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }
        // Prepare Ffmpeg to get the subtitles from video
        val pb = ProcessBuilder(
            "ffmpeg",
            "-i", videoFile.absolutePath,
            "-map", "0:s:0",
            "-f", "webvtt",
            "-"
        )
        var process: Process? = null
        try {
            // Execute the command to get subtitles
            process = pb.start()
            // Get Subtitles as a variable
            val processOutput = process.inputStream
            // Check if the video has subtitles
            val checkBuffer = ByteArray(1024)
            val bytesRead = processOutput.read(checkBuffer)
            if (bytesRead == -1) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                process.destroy()
                return
            }
            // Add WebVtt content type
            response.contentType = "text/vtt"
            response.outputStream.write(checkBuffer, 0, bytesRead)
            // Put al WebVtt in HTTP Response
            val buffer = ByteArray(8192)
            var len: Int
            while (processOutput.read(buffer).also { len = it } != -1) {
                response.outputStream.write(buffer, 0, len)
            }
            response.outputStream.flush()
            process.waitFor()

        } catch (e: Exception) {
            // If any unexpected error occurs, print stack trace and return Internal Server Error
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            process?.destroy()
        }
    }
}
package sh4dow18.miteve_api.services.genre

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.genre.GenreRequest
import sh4dow18.miteve_api.dtos.genre.GenreResponse
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.GenreMapper
import sh4dow18.miteve_api.repositories.GenreRepository

// Spring Abstract Genre Service
@Service
class AbstractGenreService(
    @Autowired
    val genreRepository: GenreRepository,
    @Autowired
    val genreMapper: GenreMapper
): GenreService {
    override fun findAll(): List<GenreResponse> {
        return genreMapper.genresListToGenreResponsesList(genreRepository.findAll())
    }
    @Transactional
    override fun insert(genreRequest: GenreRequest): GenreResponse {
        val genre = genreRepository.findByNameIgnoringCase(genreRequest.name).orElse(null)
        if (genre != null) {
            throw AlreadyExists(genreRequest.name, "Container")
        }
        val newGenre = genreMapper.genreRequestToGenre(genreRequest)
        return genreMapper.genreToGenreResponse(genreRepository.save(newGenre))
    }
    @Transactional
    override fun update(id: Long, genreRequest: GenreRequest): GenreResponse {
        val genre = genreRepository.findById(id).orElseThrow {
            NoExists("$id", "Genre")
        }
        genre.name = genreRequest.name
        return genreMapper.genreToGenreResponse(genreRepository.save(genre))
    }
}
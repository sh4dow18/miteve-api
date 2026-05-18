package sh4dow18.miteve_api.services.genre

import sh4dow18.miteve_api.dtos.genre.GenreRequest
import sh4dow18.miteve_api.dtos.genre.GenreResponse

// Genre Service Interface where the functions to be used in
// Spring Abstract Content Genre are declared
interface GenreService {
    fun findAll(): List<GenreResponse>
    fun insert(genreRequest: GenreRequest): GenreResponse
    fun update(id: Long, genreRequest: GenreRequest): GenreResponse
}
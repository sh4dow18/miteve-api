package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.genre.GenreResponse
import sh4dow18.miteve_api.entities.Genre

// Genre Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface GenreMapper {
    fun genresListToGenreResponsesList(
        genresList: List<Genre>
    ): List<GenreResponse>
}
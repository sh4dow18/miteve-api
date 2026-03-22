package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.season.SeasonResponse
import sh4dow18.miteve_api.entities.Season

// Season Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SeasonMapper {
    fun seasonToSeasonResponse(
        season: Season
    ): SeasonResponse
    fun seasonsListToSeasonResponsesList(
        seasonsList: List<Season>
    ): List<SeasonResponse>
}
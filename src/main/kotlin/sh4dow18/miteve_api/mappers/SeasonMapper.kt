package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.season.MiniSeasonResponse
import sh4dow18.miteve_api.dtos.season.SeasonRequest
import sh4dow18.miteve_api.dtos.season.SeasonResponse
import sh4dow18.miteve_api.entities.Content
import sh4dow18.miteve_api.entities.Season

// Season Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SeasonMapper {
    // Ignore id when mapping
    @Mapping(target = "id", expression = "java(id)")
    // Set series as the series sent
    @Mapping(target = "content", expression = "java(content)")
    // Set each list as empty
    @Mapping(target = "episodesList", expression = EMPTY_LIST)
    fun seasonRequestToSeason(
        id: String,
        seasonRequest: SeasonRequest,
        content: Content
    ): Season
    fun seasonToSeasonResponse(
        season: Season
    ): SeasonResponse
    fun seasonsListToSeasonResponsesList(
        seasonsList: List<Season>
    ): List<SeasonResponse>
    fun seasonsListToMiniSeasonResponsesList(
        seasonsList: List<Season>
    ): List<MiniSeasonResponse>
}
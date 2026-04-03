package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.episode.*
import sh4dow18.miteve_api.entities.Episode
import sh4dow18.miteve_api.entities.Season

// Episode Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EpisodeMapper {
    @Mapping(target = "id", expression = "java(id)")
    // Set season as the season sent
    @Mapping(target = "season", expression = "java(season)")
    fun episodeRequestToEpisode(
        id: String,
        episodeRequest: EpisodeRequest,
        season: Season
    ): Episode
    @Mapping(target = "id", expression = "java(id)")
    fun fullEpisodeRequestToEpisode(
        id: String,
        fullEpisodeRequest: FullEpisodeRequest
    ): Episode
    fun episodeToEpisodeResponse(
        episode: Episode
    ): EpisodeResponse
    fun episodeToFullEpisodeResponse(
        episode: Episode
    ): FullEpisodeResponse
    @Mapping(target = "seasonNumber", expression = "java(episode.getSeason().getSeasonNumber())")
    fun episodeToNextEpisodeResponse(
        episode: Episode
    ): NextEpisodeResponse
    fun episodeToEpisodeMetadataResponse(
        episode: Episode
    ): EpisodeMetadataResponse
    fun episodeToEpisodeMetadataResponsesList(
        episodesList: List<Episode>
    ): List<EpisodeMetadataResponse>
    fun episodesListToFullEpisodeResponsesList(
        episodesList: List<Episode>
    ): List<FullEpisodeResponse>
}
package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.episode.EpisodeMetadataResponse
import sh4dow18.miteve_api.dtos.episode.EpisodeResponse
import sh4dow18.miteve_api.dtos.episode.NextEpisodeResponse
import sh4dow18.miteve_api.entities.Episode

// Episode Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EpisodeMapper {
    fun episodeToEpisodeResponse(
        episode: Episode
    ): EpisodeResponse
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
}
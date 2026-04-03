package sh4dow18.miteve_api.services.season

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.episode.FullEpisodeResponse
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.EpisodeMapper
import sh4dow18.miteve_api.repositories.SeasonRepository

// Spring Abstract Genre Service
@Service
class AbstractSeasonService(
    @Autowired
    val seasonRepository: SeasonRepository,
    @Autowired
    val episodeMapper: EpisodeMapper
): SeasonService {
    override fun findEpisodesById(id: String): List<FullEpisodeResponse> {
        val season = seasonRepository.findById(id).orElseThrow {
            NoExists(id, "Season")
        }
        return episodeMapper.episodesListToFullEpisodeResponsesList(season.episodesList)
    }
}
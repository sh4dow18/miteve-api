package sh4dow18.miteve_api.services.episode

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.episode.*
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.EpisodeMapper
import sh4dow18.miteve_api.repositories.EpisodeRepository

// Spring Abstract Episode Service
@Service
class AbstractEpisodeService(
    @Autowired
    val episodeMapper: EpisodeMapper,
    @Autowired
    val episodeRepository: EpisodeRepository
): EpisodeService {
    override fun findNextById(id: String): NextEpisodeResponse {
        val episodeInformation = id.split("-")
        if (episodeInformation.size < 3) {
            throw BadRequest("Bad Episode Information Data")
        }
        val tvShow = episodeInformation.subList(0, episodeInformation.size - 2).joinToString("-")
        val season = episodeInformation[episodeInformation.size - 2].toIntOrNull() ?: throw BadRequest("Season has invalid data")
        val episode = episodeInformation[episodeInformation.size - 1].toIntOrNull() ?: throw BadRequest("Episode has invalid data")
        var nextEpisodeId = "${tvShow}-${season}-${episode + 1}"
        var nextEpisode = episodeRepository.findById(nextEpisodeId).orElse(null)
        if (nextEpisode == null) {
            nextEpisodeId = "${tvShow}-${season + 1}-1"
            nextEpisode = episodeRepository.findById(nextEpisodeId).orElseThrow {
                NoExists(nextEpisodeId, "Next Episode")
            }
        }
        return episodeMapper.episodeToNextEpisodeResponse(nextEpisode)
    }
    override fun findMetadataById(id: String): EpisodeMetadataResponse {
        val episode = episodeRepository.findById(id).orElseThrow {
            NoExists(id, "Episode")
        }
        return episodeMapper.episodeToEpisodeMetadataResponse(episode)
    }
    @Transactional
    override fun update(id: String, fullEpisodeRequest: FullEpisodeRequest): FullEpisodeResponse {
        val episode = episodeRepository.findById(id).orElseThrow {
            NoExists(id, "Episode")
        }
        episode.episodeNumber = fullEpisodeRequest.episodeNumber
        episode.title = fullEpisodeRequest.title
        episode.description = fullEpisodeRequest.description
        episode.beginSummary = fullEpisodeRequest.beginSummary
        episode.endSummary = fullEpisodeRequest.endSummary
        episode.beginIntro = fullEpisodeRequest.beginIntro
        episode.endIntro = fullEpisodeRequest.endIntro
        episode.beginCredits = fullEpisodeRequest.beginCredits
        return episodeMapper.episodeToFullEpisodeResponse(episodeRepository.save(episode))
    }
}
package sh4dow18.miteve_api.services.continue_watching

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.continue_watching.AddContinueWatchingRequest
import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.dtos.continue_watching.UpdateContinueWatchingTimeRequest
import sh4dow18.miteve_api.entities.ContinueWatching
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContinueWatchingMapper
import sh4dow18.miteve_api.repositories.ContentRepository
import sh4dow18.miteve_api.repositories.ContinueWatchingRepository
import sh4dow18.miteve_api.repositories.EpisodeRepository
import sh4dow18.miteve_api.repositories.ProfileRepository

// Spring Abstract ContinueWatching Service
@Service
class AbstractContinueWatchingService(
    @Autowired val continueWatchingRepository: ContinueWatchingRepository,
    @Autowired val profileRepository: ProfileRepository,
    @Autowired val contentRepository: ContentRepository,
    @Autowired val episodeRepository: EpisodeRepository,
    @Autowired val continueWatchingMapper: ContinueWatchingMapper
) : ContinueWatchingService {

    @Transactional
    override fun updateTime(id: Long, request: UpdateContinueWatchingTimeRequest): ContinueWatchingResponse {
        val continueWatching = continueWatchingRepository.findById(id).orElseThrow {
            NoExists("$id", "ContinueWatching")
        }
        continueWatching.time = request.time
        return continueWatchingMapper.continueWatchingToContinueWatchingResponse(
            continueWatchingRepository.save(continueWatching)
        )
    }

    @Transactional
    override fun addOrUpdate(request: AddContinueWatchingRequest): ContinueWatchingResponse {
        if (request.contentId == null && request.episodeId == null) {
            throw BadRequest("Either contentId or episodeId must be provided")
        }
        val profile = profileRepository.findById(request.profileId).orElseThrow {
            NoExists("${request.profileId}", "Profile")
        }
        // Resolve episode and content
        val episode = request.episodeId?.let { episodeId ->
            episodeRepository.findById(episodeId).orElseThrow {
                NoExists(episodeId, "Episode")
            }
        }
        val contentId: String = episode?.season?.content?.id ?: request.contentId!!
        val content = contentRepository.findById(contentId).orElseThrow {
            NoExists(contentId, "Content")
        }
        // Check if an entry already exists for this profile + content
        val existing = continueWatchingRepository
            .findByProfileIdAndContentId(profile.id, contentId)
            .orElse(null)
        if (existing != null) {
            existing.episode = episode
            existing.content = content
            existing.time = request.time
            return continueWatchingMapper.continueWatchingToContinueWatchingResponse(
                continueWatchingRepository.save(existing)
            )
        }
        val newEntry = ContinueWatching(
            id = 0,
            time = request.time,
            profile = profile,
            content = content,
            episode = episode
        )
        return continueWatchingMapper.continueWatchingToContinueWatchingResponse(
            continueWatchingRepository.save(newEntry)
        )
    }
}



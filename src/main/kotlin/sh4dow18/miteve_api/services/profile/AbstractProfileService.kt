package sh4dow18.miteve_api.services.profile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.dtos.profile.FullProfileResponse
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.dtos.profile.ProfileResponse
import sh4dow18.miteve_api.dtos.profile.UpdateProfileRequest
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContinueWatchingMapper
import sh4dow18.miteve_api.mappers.ContentMapper
import sh4dow18.miteve_api.mappers.ProfileMapper
import sh4dow18.miteve_api.repositories.ContentRepository
import sh4dow18.miteve_api.repositories.ProfileRepository
import sh4dow18.miteve_api.repositories.UserRepository

// Spring Abstract Profile Service
@Service
class AbstractProfileService(
    @Autowired
    val profileRepository: ProfileRepository,
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val contentRepository: ContentRepository,
    @Autowired
    val profileMapper: ProfileMapper,
    @Autowired
    val continueWatchingMapper: ContinueWatchingMapper,
    @Autowired
    val contentMapper: ContentMapper
): ProfileService {
    override fun findAllByUserId(userId: Long): List<ProfileResponse> {
        userRepository.findById(userId).orElseThrow {
            NoExists("$userId", "User")
        }
        return profileMapper.profilesListToProfileResponsesList(
            profileRepository.findAllByUserId(userId)
        )
    }
    override fun findById(id: Long): FullProfileResponse {
        val profile = profileRepository.findById(id).orElseThrow {
            NoExists("$id", "Profile")
        }
        return profileMapper.profileToFullProfileResponse(profile)
    }
    override fun findMainProfileByUserId(userId: Long): FullProfileResponse {
        userRepository.findById(userId).orElseThrow {
            NoExists("$userId", "User")
        }
        val profiles = profileRepository.findAllByUserId(userId)
        if (profiles.isEmpty()) {
            throw NoExists("$userId", "Profile for User")
        }
        return profileMapper.profileToFullProfileResponse(profiles.first())
    }
    override fun findContinueWatchingListByProfileId(profileId: Long): List<ContinueWatchingResponse> {
        val profile = profileRepository.findById(profileId).orElseThrow {
            NoExists("$profileId", "Profile")
        }
        return continueWatchingMapper.continueWatchingListToContinueWatchingResponsesList(
            profile.continueWatchingList
        )
    }
    override fun findFavoritesByProfileId(profileId: Long): List<MiniContentResponse> {
        val profile = profileRepository.findById(profileId).orElseThrow {
            NoExists("$profileId", "Profile")
        }
        return contentMapper.contentsListToMiniContentResponsesList(profile.favoritesList.toList())
    }
    override fun insert(userId: Long, profileRequest: ProfileRequest): ProfileResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NoExists("$userId", "User")
        }
        if (profileRepository.countByUserId(userId) >= 5) {
            throw BadRequest("The user with id $userId has reached the maximum limit of 5 profiles")
        }
        val profile = profileRepository.save(profileMapper.profileRequestToProfile(profileRequest, user))
        return profileMapper.profileToProfileResponse(profile)
    }
    override fun update(id: Long, request: UpdateProfileRequest): FullProfileResponse {
        val profile = profileRepository.findById(id).orElseThrow { NoExists("$id", "Profile") }
        request.name?.let { profile.name = it }
        request.autoSkip?.let { profile.autoSkip = it }
        request.lowQuality?.let { profile.lowQuality = it }
        request.disableSubtitles?.let { profile.disableSubtitles = it }
        request.adultProfile?.let { profile.adultProfile = it }
        request.allowPersonalizedRecommendations?.let { profile.allowPersonalizedRecommendations = it }
        return profileMapper.profileToFullProfileResponse(profileRepository.save(profile))
    }
    override fun addFavorite(profileId: Long, contentId: String): List<MiniContentResponse> {        val profile = profileRepository.findById(profileId).orElseThrow {
            NoExists("$profileId", "Profile")
        }
        val content = contentRepository.findById(contentId).orElseThrow {
            NoExists(contentId, "Content")
        }
        if (profile.favoritesList.contains(content)) {
            throw AlreadyExists(contentId, "Favorite")
        }
        profile.favoritesList.add(content)
        profileRepository.save(profile)
        return contentMapper.contentsListToMiniContentResponsesList(profile.favoritesList.toList())
    }
    override fun removeFavorite(profileId: Long, contentId: String): List<MiniContentResponse> {
        val profile = profileRepository.findById(profileId).orElseThrow {
            NoExists("$profileId", "Profile")
        }
        val content = contentRepository.findById(contentId).orElseThrow {
            NoExists(contentId, "Content")
        }
        if (!profile.favoritesList.contains(content)) {
            throw NoExists(contentId, "Favorite")
        }
        profile.favoritesList.remove(content)
        profileRepository.save(profile)
        return contentMapper.contentsListToMiniContentResponsesList(profile.favoritesList.toList())
    }
}


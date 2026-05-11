package sh4dow18.miteve_api.services.profile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.dtos.profile.FullProfileResponse
import sh4dow18.miteve_api.dtos.profile.ProfileResponse
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContinueWatchingMapper
import sh4dow18.miteve_api.mappers.ProfileMapper
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
    val profileMapper: ProfileMapper,
    @Autowired
    val continueWatchingMapper: ContinueWatchingMapper
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
}


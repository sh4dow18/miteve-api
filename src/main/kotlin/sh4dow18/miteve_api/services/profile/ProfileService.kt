package sh4dow18.miteve_api.services.profile

import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.dtos.profile.FullProfileResponse
import sh4dow18.miteve_api.dtos.profile.ProfileResponse

// Profile Service Interface where the functions to be used in
// Spring Abstract Profile Service are declared
interface ProfileService {
    fun findAllByUserId(userId: Long): List<ProfileResponse>
    fun findById(id: Long): FullProfileResponse
    fun findMainProfileByUserId(userId: Long): FullProfileResponse
    fun findContinueWatchingListByProfileId(profileId: Long): List<ContinueWatchingResponse>
}


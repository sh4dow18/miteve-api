package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.profile.FullProfileResponse
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.dtos.profile.ProfileResponse
import sh4dow18.miteve_api.entities.Profile
import sh4dow18.miteve_api.entities.User

// Profile Mapper
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = [ContinueWatchingMapper::class]
)
interface ProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "continueWatchingList", expression = EMPTY_LIST)
    @Mapping(target = "user", expression = "java(user)")
    fun profileRequestToProfile(
        profileRequest: ProfileRequest,
        user: User
    ): Profile
    fun profileToProfileResponse(profile: Profile): ProfileResponse
    fun profilesListToProfileResponsesList(profiles: List<Profile>): List<ProfileResponse>
    fun profileToFullProfileResponse(profile: Profile): FullProfileResponse
}
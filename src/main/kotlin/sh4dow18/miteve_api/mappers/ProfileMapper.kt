package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.entities.Profile
import sh4dow18.miteve_api.entities.User

// User Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "continueWatchingList", expression = EMPTY_LIST)
    @Mapping(target = "user", expression = "java(user)")
    fun profileRequestToProfile(
        profileRequest: ProfileRequest,
        user: User
    ): Profile
}
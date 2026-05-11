package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.user.RegisterUserResponse
import sh4dow18.miteve_api.dtos.user.UserRequest
import sh4dow18.miteve_api.entities.Role
import sh4dow18.miteve_api.entities.User

// User Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profilesList", expression = EMPTY_LIST)
    @Mapping(target = "role", expression = "java(role)")
    @Mapping(target = "password", expression = "java(encodedPassword)")
    fun userRequestToUser(
        userRequest: UserRequest,
        role: Role,
        encodedPassword: String
    ): User
    fun userToRegisterUserResponse(
        user: User
    ): RegisterUserResponse
}
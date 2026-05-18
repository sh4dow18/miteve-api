package sh4dow18.miteve_api.services.user

import sh4dow18.miteve_api.dtos.user.RegisterUserResponse
import sh4dow18.miteve_api.dtos.user.UserRequest
import sh4dow18.miteve_api.dtos.user.UserResponse

// User Service Interface where the functions to be used in
// Spring Abstract Content User are declared
interface UserService {
    fun register(userRequest: UserRequest): RegisterUserResponse
    fun findById(id: Long): UserResponse
}
package sh4dow18.miteve_api.services.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.dtos.user.RegisterUserResponse
import sh4dow18.miteve_api.dtos.user.UserRequest
import sh4dow18.miteve_api.dtos.user.UserResponse
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ProfileMapper
import sh4dow18.miteve_api.mappers.UserMapper
import sh4dow18.miteve_api.repositories.ProfileRepository
import sh4dow18.miteve_api.repositories.RoleRepository
import sh4dow18.miteve_api.repositories.UserRepository

// Spring Abstract User Service
@Service
class AbstractUserService(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val roleRepository: RoleRepository,
    @Autowired
    val profileRepository: ProfileRepository,
    @Autowired
    val userMapper: UserMapper,
    @Autowired
    val profileMapper: ProfileMapper
): UserService {
    // Encode Passwords
    private fun passwordEncoder(password: String): String =
        BCryptPasswordEncoder().encode(password) ?: throw IllegalStateException("Error encoding password")
    override fun register(userRequest: UserRequest): RegisterUserResponse {
        if (userRepository.findByEmail(userRequest.email).orElse(null) != null) {
            throw AlreadyExists(userRequest.email, "User")
        }
        val role = roleRepository.findById(2).orElseThrow {
            NoExists("Client", "Role")
        }
        val user = userRepository.save(userMapper.userRequestToUser(userRequest, role, passwordEncoder(userRequest.password)))
        profileRepository.save(profileMapper.profileRequestToProfile(ProfileRequest(userRequest.name), user))
        return userMapper.userToRegisterUserResponse(user)
    }
    override fun findById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            NoExists("$id", "User")
        }
        return userMapper.userToUserResponse(user)
    }
}
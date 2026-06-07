package sh4dow18.miteve_api.services.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.dtos.user.RegisterUserResponse
import sh4dow18.miteve_api.dtos.user.UpdateUserRequest
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
    private fun passwordEncoder(password: String): String =
        BCryptPasswordEncoder().encode(password) ?: throw IllegalStateException("Error encoding password")

    override fun findAll(): List<UserResponse> =
        userMapper.usersListToUserResponsesList(userRepository.findAll())

    override fun findById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            NoExists("$id", "User")
        }
        return userMapper.userToUserResponse(user)
    }

    @Transactional
    override fun register(userRequest: UserRequest): RegisterUserResponse {
        if (userRepository.findByEmail(userRequest.email).orElse(null) != null) {
            throw AlreadyExists(userRequest.email, "User")
        }
        val role = roleRepository.findById(userRequest.roleId).orElseThrow {
            NoExists("${userRequest.roleId}", "Role")
        }
        val user = userRepository.save(userMapper.userRequestToUser(userRequest, role, passwordEncoder(userRequest.password)))
        profileRepository.save(profileMapper.profileRequestToProfile(ProfileRequest(userRequest.name), user))
        return userMapper.userToRegisterUserResponse(user)
    }

    @Transactional
    override fun update(id: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            NoExists("$id", "User")
        }
        request.email?.let {
            if (it != user.email && userRepository.findByEmail(it).isPresent) throw AlreadyExists(it, "User")
            user.email = it
        }
        request.password?.let { user.password = passwordEncoder(it) }
        request.roleId?.let {
            user.role = roleRepository.findById(it).orElseThrow { NoExists("$it", "Role") }
        }
        return userMapper.userToUserResponse(userRepository.save(user))
    }

    @Transactional
    override fun delete(id: Long) {
        userRepository.findById(id).orElseThrow { NoExists("$id", "User") }
        userRepository.deleteById(id)
    }
}
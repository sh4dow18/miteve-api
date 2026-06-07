package sh4dow18.miteve_api.dtos.user

data class UpdateUserRequest(
    val email: String? = null,
    val password: String? = null,
    val roleId: Long? = null
)

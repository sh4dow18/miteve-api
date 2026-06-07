package sh4dow18.miteve_api.dtos.user

data class UserRequest(
    var roleId: Long,
    var name: String,
    var email: String,
    var password: String,
)

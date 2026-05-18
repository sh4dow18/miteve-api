package sh4dow18.miteve_api.dtos.user

import sh4dow18.miteve_api.dtos.profile.MiniProfileResponse
import sh4dow18.miteve_api.dtos.role.MiniRoleResponse

data class UserResponse(
    var id: Long,
    var email: String,
    var profilesList: List<MiniProfileResponse>,
    var role: MiniRoleResponse,
)


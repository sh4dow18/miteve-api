package sh4dow18.miteve_api.dtos.role

import sh4dow18.miteve_api.dtos.privilege.MiniPrivilegeResponse

data class RoleResponse(
    val id: Long,
    val name: String,
    val privilegesList: List<MiniPrivilegeResponse>
)

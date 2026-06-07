package sh4dow18.miteve_api.dtos.role

data class UpdateRoleRequest(
    val name: String? = null,
    val privilegeIds: List<Long>? = null
)

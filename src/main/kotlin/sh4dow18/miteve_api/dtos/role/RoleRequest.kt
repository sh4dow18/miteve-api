package sh4dow18.miteve_api.dtos.role

data class RoleRequest(
    val name: String,
    val privilegeIds: List<Long>
)

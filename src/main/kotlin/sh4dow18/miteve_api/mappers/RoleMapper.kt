package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.role.RoleResponse
import sh4dow18.miteve_api.entities.Role

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = [PrivilegeMapper::class])
interface RoleMapper {
    fun roleToRoleResponse(role: Role): RoleResponse
    fun rolesToRoleResponsesList(roles: List<Role>): List<RoleResponse>
}

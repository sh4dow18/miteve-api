package sh4dow18.miteve_api.services.role

import sh4dow18.miteve_api.dtos.role.RoleRequest
import sh4dow18.miteve_api.dtos.role.RoleResponse
import sh4dow18.miteve_api.dtos.role.UpdateRoleRequest

interface RoleService {
    fun findAll(): List<RoleResponse>
    fun findById(id: Long): RoleResponse
    fun insert(request: RoleRequest): RoleResponse
    fun update(id: Long, request: UpdateRoleRequest): RoleResponse
    fun delete(id: Long)
}

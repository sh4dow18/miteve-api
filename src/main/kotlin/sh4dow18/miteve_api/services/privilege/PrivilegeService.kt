package sh4dow18.miteve_api.services.privilege

import sh4dow18.miteve_api.dtos.privilege.PrivilegeRequest
import sh4dow18.miteve_api.dtos.privilege.PrivilegeResponse
import sh4dow18.miteve_api.dtos.privilege.UpdatePrivilegeRequest

interface PrivilegeService {
    fun findAll(): List<PrivilegeResponse>
    fun findById(id: Long): PrivilegeResponse
    fun insert(request: PrivilegeRequest): PrivilegeResponse
    fun update(id: Long, request: UpdatePrivilegeRequest): PrivilegeResponse
    fun delete(id: Long)
}

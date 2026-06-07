package sh4dow18.miteve_api.services.role

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.role.RoleRequest
import sh4dow18.miteve_api.dtos.role.RoleResponse
import sh4dow18.miteve_api.dtos.role.UpdateRoleRequest
import sh4dow18.miteve_api.entities.Role
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.RoleMapper
import sh4dow18.miteve_api.repositories.PrivilegeRepository
import sh4dow18.miteve_api.repositories.RoleRepository

@Service
class AbstractRoleService(
    @Autowired val roleRepository: RoleRepository,
    @Autowired val privilegeRepository: PrivilegeRepository,
    @Autowired val roleMapper: RoleMapper
) : RoleService {

    override fun findAll(): List<RoleResponse> =
        roleMapper.rolesToRoleResponsesList(roleRepository.findAll())

    override fun findById(id: Long): RoleResponse {
        val role = roleRepository.findById(id).orElseThrow { NoExists("$id", "Role") }
        return roleMapper.roleToRoleResponse(role)
    }

    @Transactional
    override fun insert(request: RoleRequest): RoleResponse {
        val privileges = request.privilegeIds.map { pid ->
            privilegeRepository.findById(pid).orElseThrow { NoExists("$pid", "Privilege") }
        }.toSet()
        val role = roleRepository.save(
            Role(id = 0, name = request.name, usersList = emptyList(), privilegesList = privileges)
        )
        return roleMapper.roleToRoleResponse(role)
    }

    @Transactional
    override fun update(id: Long, request: UpdateRoleRequest): RoleResponse {
        val role = roleRepository.findById(id).orElseThrow { NoExists("$id", "Role") }
        request.name?.let { role.name = it }
        request.privilegeIds?.let { ids ->
            role.privilegesList = ids.map { pid ->
                privilegeRepository.findById(pid).orElseThrow { NoExists("$pid", "Privilege") }
            }.toSet()
        }
        return roleMapper.roleToRoleResponse(roleRepository.save(role))
    }

    @Transactional
    override fun delete(id: Long) {
        roleRepository.findById(id).orElseThrow { NoExists("$id", "Role") }
        roleRepository.deleteById(id)
    }
}

package sh4dow18.miteve_api.services.privilege

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.privilege.PrivilegeRequest
import sh4dow18.miteve_api.dtos.privilege.PrivilegeResponse
import sh4dow18.miteve_api.dtos.privilege.UpdatePrivilegeRequest
import sh4dow18.miteve_api.entities.Privilege
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.PrivilegeMapper
import sh4dow18.miteve_api.repositories.PrivilegeRepository

@Service
class AbstractPrivilegeService(
    @Autowired val privilegeRepository: PrivilegeRepository,
    @Autowired val privilegeMapper: PrivilegeMapper
) : PrivilegeService {

    override fun findAll(): List<PrivilegeResponse> =
        privilegeMapper.privilegesListToPrivilegeResponsesList(privilegeRepository.findAll())

    override fun findById(id: Long): PrivilegeResponse {
        val privilege = privilegeRepository.findById(id).orElseThrow { NoExists("$id", "Privilege") }
        return privilegeMapper.privilegeToPrivilegeResponse(privilege)
    }

    @Transactional
    override fun insert(request: PrivilegeRequest): PrivilegeResponse {
        val privilege = privilegeRepository.save(
            Privilege(id = 0, slug = request.slug, name = request.name, rolesList = emptySet())
        )
        return privilegeMapper.privilegeToPrivilegeResponse(privilege)
    }

    @Transactional
    override fun update(id: Long, request: UpdatePrivilegeRequest): PrivilegeResponse {
        val privilege = privilegeRepository.findById(id).orElseThrow { NoExists("$id", "Privilege") }
        request.slug?.let { privilege.slug = it }
        request.name?.let { privilege.name = it }
        return privilegeMapper.privilegeToPrivilegeResponse(privilegeRepository.save(privilege))
    }

    @Transactional
    override fun delete(id: Long) {
        privilegeRepository.findById(id).orElseThrow { NoExists("$id", "Privilege") }
        privilegeRepository.deleteById(id)
    }
}

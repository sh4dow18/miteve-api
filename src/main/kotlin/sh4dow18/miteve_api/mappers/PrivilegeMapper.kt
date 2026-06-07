package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.privilege.MiniPrivilegeResponse
import sh4dow18.miteve_api.dtos.privilege.PrivilegeResponse
import sh4dow18.miteve_api.entities.Privilege

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PrivilegeMapper {
    fun privilegeToPrivilegeResponse(privilege: Privilege): PrivilegeResponse
    fun privilegeToMiniPrivilegeResponse(privilege: Privilege): MiniPrivilegeResponse
    fun privilegesListToPrivilegeResponsesList(privileges: List<Privilege>): List<PrivilegeResponse>
}

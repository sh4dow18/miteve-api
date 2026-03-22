package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.container.ContainerResponse
import sh4dow18.miteve_api.entities.Container

// Content Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContainerMapper {
    fun containersListToContainerResponsesList(
        containersList: List<Container>
    ): List<ContainerResponse>
}
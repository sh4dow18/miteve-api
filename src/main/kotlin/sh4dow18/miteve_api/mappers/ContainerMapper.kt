package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.container.ContainerRequest
import sh4dow18.miteve_api.dtos.container.ContainerResponse
import sh4dow18.miteve_api.dtos.container.MiniContainerResponse
import sh4dow18.miteve_api.entities.Container

// Content Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContainerMapper {
    @Mapping(target = "elementsList", expression = EMPTY_LIST)
    fun containerRequestToContainer(
        containerRequest: ContainerRequest
    ): Container
    fun containerToContainerResponse(
        container: Container
    ): ContainerResponse
    fun containersListToMiniContainerResponsesList(
        containersList: List<Container>
    ): List<MiniContainerResponse>
    fun containersListToContainerResponsesList(
        containersList: List<Container>
    ): List<ContainerResponse>
}
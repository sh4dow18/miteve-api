package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.container_element.ContainerElementResponse
import sh4dow18.miteve_api.entities.Container
import sh4dow18.miteve_api.entities.ContainerElement
import sh4dow18.miteve_api.entities.Content

// Content Element Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContainerElementMapper {
    // Ignore id when mapping
    @Mapping(target = "id", ignore = true)
    fun contentElementRequestToContainerElement(
        position: Short,
        content: Content,
        container: Container
    ): ContainerElement
    fun containerElementsListToContainerElementResponsesList(
        containerElementsList: List<ContainerElement>
    ): List<ContainerElementResponse>
}
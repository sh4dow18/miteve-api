package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.container_element.ContainerElementResponse
import sh4dow18.miteve_api.entities.ContainerElement

// Content Element Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContainerElementMapper {
    fun containerElementsListToContainerElementResponsesList(
        containerElementsList: List<ContainerElement>
    ): List<ContainerElementResponse>
}
package sh4dow18.miteve_api.dtos.container

import sh4dow18.miteve_api.dtos.container_element.ContainerElementResponse

data class ContainerResponse(
    var id: Long,
    var name: String,
    var elementsList: List<ContainerElementResponse>
)

package sh4dow18.miteve_api.dtos.container_element

import sh4dow18.miteve_api.dtos.content.MiniContentResponse

data class ContainerElementResponse(
    var id: Long,
    var position: Short,
    var content: MiniContentResponse?
)

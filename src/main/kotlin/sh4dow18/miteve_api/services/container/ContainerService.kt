package sh4dow18.miteve_api.services.container

import sh4dow18.miteve_api.dtos.container.ContainerResponse

// Container Service Interface where the functions to be used in
// Spring Abstract Container Service are declared
interface ContainerService {
    fun findByContentType(typeId: Long): List<ContainerResponse>
}
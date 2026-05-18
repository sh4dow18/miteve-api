package sh4dow18.miteve_api.services.container

import sh4dow18.miteve_api.dtos.container.ContainerRequest
import sh4dow18.miteve_api.dtos.container.ContainerResponse
import sh4dow18.miteve_api.dtos.container.MiniContainerResponse

// Container Service Interface where the functions to be used in
// Spring Abstract Container Service are declared
interface ContainerService {
    fun findAll(): List<MiniContainerResponse>
    fun findByContentType(typeId: Long): List<ContainerResponse>
    fun insert(containerRequest: ContainerRequest): ContainerResponse
    fun update(id: Long, containerRequest: ContainerRequest): ContainerResponse
}
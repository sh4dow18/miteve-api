package sh4dow18.miteve_api.services.container

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.container.ContainerRequest
import sh4dow18.miteve_api.dtos.container.ContainerResponse
import sh4dow18.miteve_api.dtos.container.MiniContainerResponse
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContainerMapper
import sh4dow18.miteve_api.repositories.ContainerRepository

// Spring Abstract Container Service
@Service
class AbstractContainerService(
    @Autowired
    val containerRepository: ContainerRepository,
    @Autowired
    val containerMapper: ContainerMapper,
): ContainerService {
    override fun findAll(): List<MiniContainerResponse> {
        return containerMapper.containersListToMiniContainerResponsesList(containerRepository.findAll())
    }
    override fun findByContentType(typeId: Long): List<ContainerResponse> {
        return containerMapper.containersListToContainerResponsesList(containerRepository.findContainersWithOnlyThisType(typeId))
    }
    @Transactional
    override fun insert(containerRequest: ContainerRequest): ContainerResponse{
        val container = containerRepository.findByNameIgnoringCase(containerRequest.name).orElse(null)
        if (container != null) {
            throw AlreadyExists(containerRequest.name, "Container")
        }
        val newContainer = containerMapper.containerRequestToContainer(containerRequest)
        return containerMapper.containerToContainerResponse(containerRepository.save(newContainer))
    }
    @Transactional
    override fun update(id: Long, containerRequest: ContainerRequest): ContainerResponse {
        val container = containerRepository.findById(id).orElseThrow {
            NoExists("$id", "Container")
        }
        container.name = containerRequest.name
        return containerMapper.containerToContainerResponse(containerRepository.save(container))
    }
}
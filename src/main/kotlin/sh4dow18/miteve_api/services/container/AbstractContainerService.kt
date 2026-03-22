package sh4dow18.miteve_api.services.container

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.container.ContainerResponse
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
    override fun findByContentType(typeId: Long): List<ContainerResponse> {
        return containerMapper.containersListToContainerResponsesList(containerRepository.findContainersWithOnlyThisType(typeId))
    }
}
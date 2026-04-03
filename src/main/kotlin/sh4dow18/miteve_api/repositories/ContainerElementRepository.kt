package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Container
import sh4dow18.miteve_api.entities.ContainerElement

@Repository
interface ContainerElementRepository: JpaRepository<ContainerElement, Long> {
    @Modifying
    @Query("UPDATE ContainerElement ce SET ce.position = ce.position + 1 WHERE ce.container = :container AND ce.position >= :position")
    fun shiftPositionFrom(container: Container, position: Short)
}
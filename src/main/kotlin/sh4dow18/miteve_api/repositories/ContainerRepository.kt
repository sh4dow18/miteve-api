package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Container

@Repository
interface ContainerRepository: JpaRepository<Container, String> {
    @Query("""
    SELECT c FROM Container c
    WHERE NOT EXISTS (
        SELECT e FROM ContainerElement e
        WHERE e.container = c
        AND e.content IS NOT NULL
        AND e.content.type.id <> :typeId
    )
    """)
    fun findContainersWithOnlyThisType(@Param("typeId") typeId: Long): List<Container>
}
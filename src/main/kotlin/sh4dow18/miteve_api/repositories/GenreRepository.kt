package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Genre
import java.util.Optional

@Repository
interface GenreRepository: JpaRepository<Genre, Long> {
    fun findByNameIgnoringCase(@Param("name") name: String): Optional<Genre>
}
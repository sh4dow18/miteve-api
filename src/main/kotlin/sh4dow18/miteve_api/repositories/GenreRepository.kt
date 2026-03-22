package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Genre

@Repository
interface GenreRepository: JpaRepository<Genre, Long>
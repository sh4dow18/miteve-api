package sh4dow18.miteve_api
// Repositories Requirements
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
// Genre Repository
@Repository
interface GenreRepository: JpaRepository<Genre, Long>
// Movie Repository
@Repository
interface MovieRepository: JpaRepository<Movie, Long> {
    // Find Movie by TMDB Id
    fun findByTmdbId(@Param("tmdbId") tmdbId: Long): Optional<Movie>
}
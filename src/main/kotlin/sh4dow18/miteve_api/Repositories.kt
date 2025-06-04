package sh4dow18.miteve_api
// Repositories Requirements
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
// Genre Repository
@Repository
interface GenreRepository: JpaRepository<Genre, Long> {
    fun findByNameIgnoreCase(@Param("name") name: String): Optional<Genre>
    @Query("SELECT g.seriesList FROM Genre g WHERE LOWER(g.name) = LOWER(:name)")
    fun findSeriesByGenreNameIgnoreCase(@Param("name") name: String): List<Series>
}
// Movie Repository
@Repository
interface MovieRepository: JpaRepository<Movie, Long> {
    fun findByCollectionAndIdNot(@Param("collection") collection: String?, @Param("id") id: Long): List<Movie>
    fun findTop10ByGenresListIdAndCollectionNot(@Param("id") id: Long, @Param("collection") collection: String?): List<Movie>
}
// Series Repository
@Repository
interface SeriesRepository: JpaRepository<Series, Long>
// Season Repository
@Repository
interface SeasonRepository: JpaRepository<Season, Long>
// Episode Repository
@Repository
interface EpisodeRepository: JpaRepository<Episode, Long>
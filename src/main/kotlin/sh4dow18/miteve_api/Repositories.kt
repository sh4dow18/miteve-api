package sh4dow18.miteve_api
// Repositories Requirements
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
// Genre Repository
@Repository
interface GenreRepository: JpaRepository<Genre, Long> {
    fun findByNameIgnoreCase(@Param("name") name: String): Optional<Genre>
    @Query("SELECT s FROM Genre g JOIN g.seriesList s WHERE LOWER(g.name) = LOWER(:name) AND s.id <> :id")
    fun findSeriesByGenreNameIgnoreCaseAndIdNot(@Param("name") name: String, @Param("id") id: Long): List<Series>
}
// Movie Repository
@Repository
interface MovieRepository: JpaRepository<Movie, Long> {
    fun findByCollectionAndIdNot(@Param("collection") collection: String?, @Param("id") id: Long): List<Movie>
    fun findTop10ByGenresListIdAndCollectionNot(@Param("id") id: Long, @Param("collection") collection: String?): List<Movie>
    fun findByTitleContainingIgnoreCase(@Param("title") title: String): List<Movie>
}
// Series Repository
@Repository
interface SeriesRepository: JpaRepository<Series, Long> {
    fun findByTitleContainingIgnoreCase(@Param("title") title: String): List<Series>
}
// Season Repository
@Repository
interface SeasonRepository: JpaRepository<Season, Long>
// Episode Repository
@Repository
interface EpisodeRepository: JpaRepository<Episode, Long>
// Container Repository
@Repository
interface ContainerRepository: JpaRepository<Container, Long> {
    fun findAllByType(@Param("type") type: String): List<Container>
    fun findContainerByNameIgnoreCase(@Param("name") name: String): Optional<Container>
}
// Container Element Repository
@Repository
interface ContainerElementRepository: JpaRepository<ContainerElement, Long> {
    @Modifying
    @Query("UPDATE ContainerElement ce SET ce.orderNumber = ce.orderNumber + 1 WHERE ce.container = :container AND ce.orderNumber >= :order")
    fun shiftOrderNumberFrom(container: Container, order: Int)
}
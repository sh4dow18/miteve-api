package sh4dow18.miteve_api.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.SuggestedContentReport

@Repository
interface SuggestedContentReportRepository : JpaRepository<SuggestedContentReport, Long> {
    fun findAllByOrderByReportedAtDesc(): List<SuggestedContentReport>
    fun findAllByUserId(userId: Long): List<SuggestedContentReport>
    fun existsByTmdbIdAndContentTypeId(tmdbId: Long, contentTypeId: Long): Boolean
    fun findByStatusNameNotIn(names: Collection<String>, pageable: Pageable): Page<SuggestedContentReport>
    fun findByStatusNameIn(names: Collection<String>, pageable: Pageable): Page<SuggestedContentReport>
    @Query("""
        SELECT r FROM SuggestedContentReport r
        WHERE r.status.name NOT IN :rejected
        AND NOT (LOWER(r.status.name) = 'subido' AND EXISTS (SELECT 1 FROM Content c WHERE c.tmdbId = r.tmdbId))
    """)
    fun findActiveReports(@Param("rejected") rejected: Collection<String>, pageable: Pageable): Page<SuggestedContentReport>
}

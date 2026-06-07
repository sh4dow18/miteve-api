package sh4dow18.miteve_api.repositories

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Content
import sh4dow18.miteve_api.entities.History

@Repository
interface HistoryRepository : JpaRepository<History, Long> {
    fun findAllByProfileId(profileId: Long): List<History>
    fun findByProfileIdAndContentId(profileId: Long, contentId: String): java.util.Optional<History>
    fun findTop15ByProfileIdOrderByViewedAtAsc(profileId: Long): List<History>
    @Query("""
        SELECT h.content FROM History h
        WHERE h.content.comingSoon = false
        GROUP BY h.content
        ORDER BY SUM(h.viewCount) DESC
    """)
    fun findTopWatchedContents(pageable: Pageable): List<Content>
}

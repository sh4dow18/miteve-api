package sh4dow18.miteve_api.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Content

@Repository
interface ContentRepository: JpaRepository<Content, String> {
    fun findTop10ByComingSoonFalseOrderByCreatedDateDesc(): List<Content>
    @Query("""
        SELECT DISTINCT c FROM Content c LEFT JOIN c.seasonsList s
        WHERE c.comingSoon = true OR s.comingSoon = true
        ORDER BY c.createdDate DESC
    """)
    fun findComingSoon(): List<Content>
    fun findByTitleContainingIgnoreCase(@Param("title") title: String): List<Content>
    fun findByTitleContainingIgnoreCase(@Param("title") title: String, pageable: Pageable): Page<Content>
    fun findByGenresListId(@Param("genreId") genreId: Long, pageable: Pageable): Page<Content>
    @Query(
        value = """
            SELECT DISTINCT c FROM Content c JOIN c.genresList g
            WHERE c.id <> :id
            AND c.comingSoon = false
            AND (g.id IN :genreIds OR LOWER(c.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')))
            ORDER BY c.createdDate DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT c) FROM Content c JOIN c.genresList g
            WHERE c.id <> :id
            AND c.comingSoon = false
            AND (g.id IN :genreIds OR LOWER(c.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')))
        """
    )
    fun findSimilarContent(
        @Param("id") id: String,
        @Param("genreIds") genreIds: Set<Long>,
        @Param("titleKeyword") titleKeyword: String,
        pageable: Pageable
    ): Page<Content>
    @Query("""
        SELECT DISTINCT c FROM Content c JOIN c.genresList g
        WHERE c.id NOT IN :watchedIds
        AND c.comingSoon = false
        AND g.id IN :genreIds
        ORDER BY c.createdDate DESC
    """)
    fun findRecommendedContent(
        @Param("watchedIds") watchedIds: Set<String>,
        @Param("genreIds") genreIds: Set<Long>,
        pageable: Pageable
    ): List<Content>
}
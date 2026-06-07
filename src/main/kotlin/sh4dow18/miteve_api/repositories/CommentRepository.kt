package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.Comment

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    fun findAllByContentId(contentId: String): List<Comment>
    fun existsByProfileIdAndContentId(profileId: Long, contentId: String): Boolean
}

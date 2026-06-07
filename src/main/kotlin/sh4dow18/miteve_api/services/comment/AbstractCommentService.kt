package sh4dow18.miteve_api.services.comment

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.comment.CommentRequest
import sh4dow18.miteve_api.dtos.comment.CommentResponse
import sh4dow18.miteve_api.dtos.comment.UpdateCommentRequest
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.CommentMapper
import sh4dow18.miteve_api.repositories.CommentRepository
import sh4dow18.miteve_api.repositories.ContentRepository
import sh4dow18.miteve_api.repositories.ProfileRepository

// Spring Abstract Comment Service
@Service
class AbstractCommentService(
    @Autowired val commentRepository: CommentRepository,
    @Autowired val contentRepository: ContentRepository,
    @Autowired val profileRepository: ProfileRepository,
    @Autowired val commentMapper: CommentMapper
) : CommentService {

    override fun findAllByContentId(contentId: String): List<CommentResponse> {
        if (!contentRepository.existsById(contentId)) {
            throw NoExists(contentId, "Content")
        }
        return commentMapper.commentsListToCommentResponsesList(
            commentRepository.findAllByContentId(contentId)
        )
    }

    @Transactional
    override fun insert(commentRequest: CommentRequest): CommentResponse {
        if (commentRequest.rating !in 1..10) {
            throw BadRequest("Rating must be between 1 and 10")
        }
        val profile = profileRepository.findById(commentRequest.profileId).orElseThrow {
            NoExists("${commentRequest.profileId}", "Profile")
        }
        val content = contentRepository.findById(commentRequest.contentId).orElseThrow {
            NoExists(commentRequest.contentId, "Content")
        }
        if (commentRepository.existsByProfileIdAndContentId(profile.id, content.id)) {
            throw AlreadyExists("${profile.id} on ${content.id}", "Comment")
        }
        return commentMapper.commentToCommentResponse(
            commentRepository.save(commentMapper.commentRequestToComment(commentRequest, profile, content))
        )
    }
    @Transactional
    override fun update(id: Long, updateCommentRequest: UpdateCommentRequest): CommentResponse {
        if (updateCommentRequest.rating < 1 || updateCommentRequest.rating > 10) {
            throw BadRequest("Rating must be between 1 and 10")
        }
        val comment = commentRepository.findById(id).orElseThrow {
            NoExists("$id", "Comment")
        }
        comment.message = updateCommentRequest.message
        comment.rating = updateCommentRequest.rating
        return commentMapper.commentToCommentResponse(commentRepository.save(comment))
    }
}

package sh4dow18.miteve_api.services.comment

import sh4dow18.miteve_api.dtos.comment.CommentRequest
import sh4dow18.miteve_api.dtos.comment.CommentResponse
import sh4dow18.miteve_api.dtos.comment.UpdateCommentRequest

// Comment Service Interface where the functions to be used in
// Spring Abstract Comment Service are declared
interface CommentService {
    fun findAllByContentId(contentId: String): List<CommentResponse>
    fun insert(commentRequest: CommentRequest): CommentResponse
    fun update(id: Long, updateCommentRequest: UpdateCommentRequest): CommentResponse
}

package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.comment.CommentRequest
import sh4dow18.miteve_api.dtos.comment.CommentResponse
import sh4dow18.miteve_api.entities.Comment
import sh4dow18.miteve_api.entities.Content
import sh4dow18.miteve_api.entities.Profile

// Comment Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.ZonedDateTime.now())")
    @Mapping(target = "profile", expression = "java(profile)")
    @Mapping(target = "content", expression = "java(content)")
    @Mapping(target = "rating", source = "commentRequest.rating")
    @Mapping(target = "message", source = "commentRequest.message")
    fun commentRequestToComment(commentRequest: CommentRequest, profile: Profile, content: Content): Comment
    @Mapping(target = "profileId", expression = "java(comment.getProfile().getId())")
    @Mapping(target = "profileName", expression = "java(comment.getProfile().getName())")
    fun commentToCommentResponse(comment: Comment): CommentResponse
    fun commentsListToCommentResponsesList(commentsList: List<Comment>): List<CommentResponse>
}

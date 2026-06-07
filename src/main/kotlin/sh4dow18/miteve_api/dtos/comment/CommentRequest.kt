package sh4dow18.miteve_api.dtos.comment

data class CommentRequest(
    var profileId: Long,
    var contentId: String,
    var message: String,
    var rating: Int
)

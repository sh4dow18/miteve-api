package sh4dow18.miteve_api.dtos.comment

import java.time.ZonedDateTime

data class CommentResponse(
    var id: Long,
    var message: String,
    var rating: Int,
    var profileId: Long,
    var profileName: String,
    var createdAt: ZonedDateTime
)

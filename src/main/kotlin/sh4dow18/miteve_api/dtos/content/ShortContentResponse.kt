package sh4dow18.miteve_api.dtos.content

import java.time.ZonedDateTime

data class ShortContentResponse(
    var id: String,
    var title: String,
    var year: Short,
    var rating: Float,
    var age: Short,
    var comingSoon: Boolean,
    var createdDate: String,
    var type: String,
)

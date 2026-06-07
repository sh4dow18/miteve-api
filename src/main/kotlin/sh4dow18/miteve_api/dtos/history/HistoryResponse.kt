package sh4dow18.miteve_api.dtos.history

import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import java.time.ZonedDateTime

data class HistoryResponse(
    var id: Long,
    var content: MiniContentResponse,
    var time: Long,
    var viewedAt: ZonedDateTime,
    var viewCount: Int,
)

package sh4dow18.miteve_api.dtos.suggested_content_report

import java.time.ZonedDateTime

data class SuggestedContentReportResponse(
    val id: Long,
    val message: String,
    val reportedAt: ZonedDateTime,
    val userId: Long,
    val userEmail: String,
    val statusId: Long,
    val statusName: String,
    val tmdbId: Long,
    val rejectionReason: String?
)

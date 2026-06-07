package sh4dow18.miteve_api.dtos.bug_report

import java.time.ZonedDateTime

data class BugReportResponse(
    var id: Long,
    var message: String,
    var reportedAt: ZonedDateTime,
    var userId: Long,
    var userEmail: String,
    var statusId: Long,
    var statusName: String
)

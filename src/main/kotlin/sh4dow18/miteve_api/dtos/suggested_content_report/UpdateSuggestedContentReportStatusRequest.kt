package sh4dow18.miteve_api.dtos.suggested_content_report

data class UpdateSuggestedContentReportStatusRequest(
    val statusId: Long,
    val rejectionReason: String? = null
)

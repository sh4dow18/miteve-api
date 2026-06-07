package sh4dow18.miteve_api.services.suggested_content_report

import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportRequest
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportResponse
import sh4dow18.miteve_api.dtos.suggested_content_report.UpdateSuggestedContentReportStatusRequest

interface SuggestedContentReportService {
    fun findAll(): List<SuggestedContentReportResponse>
    fun findById(id: Long): SuggestedContentReportResponse
    fun findAllByUserId(userId: Long): List<SuggestedContentReportResponse>
    fun insert(request: SuggestedContentReportRequest): SuggestedContentReportResponse
    fun updateStatus(id: Long, request: UpdateSuggestedContentReportStatusRequest): SuggestedContentReportResponse
}

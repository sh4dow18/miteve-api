package sh4dow18.miteve_api.services.suggested_content_report

import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportRequest
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportResponse
import sh4dow18.miteve_api.dtos.suggested_content_report.UpdateSuggestedContentReportStatusRequest

import org.springframework.data.domain.Page

interface SuggestedContentReportService {
    fun findAll(page: Int, size: Int): Page<SuggestedContentReportResponse>
    fun findRejected(page: Int, size: Int): Page<SuggestedContentReportResponse>
    fun findById(id: Long): SuggestedContentReportResponse
    fun findAllByUserId(userId: Long): List<SuggestedContentReportResponse>
    fun insert(request: SuggestedContentReportRequest): SuggestedContentReportResponse
    fun updateStatus(id: Long, request: UpdateSuggestedContentReportStatusRequest): SuggestedContentReportResponse
    fun existsByTmdbIdAndContentTypeId(tmdbId: Long, contentTypeId: Long): Boolean
}

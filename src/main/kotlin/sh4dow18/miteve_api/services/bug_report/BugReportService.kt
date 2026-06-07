package sh4dow18.miteve_api.services.bug_report

import sh4dow18.miteve_api.dtos.bug_report.BugReportRequest
import sh4dow18.miteve_api.dtos.bug_report.BugReportResponse
import sh4dow18.miteve_api.dtos.bug_report.UpdateBugReportStatusRequest

// Bug Report Service Interface where the functions to be used in
// Spring Abstract Bug Report Service are declared
interface BugReportService {
    fun findAll(): List<BugReportResponse>
    fun findById(id: Long): BugReportResponse
    fun findAllByUserId(userId: Long): List<BugReportResponse>
    fun insert(request: BugReportRequest): BugReportResponse
    fun updateStatus(id: Long, request: UpdateBugReportStatusRequest): BugReportResponse
}

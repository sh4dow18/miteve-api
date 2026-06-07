package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.bug_report.BugReportResponse
import sh4dow18.miteve_api.entities.BugReport
import sh4dow18.miteve_api.entities.BugReportStatus
import sh4dow18.miteve_api.entities.User

// Bug Report Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface BugReportMapper {
    @Mapping(target = "userId", expression = "java(bugReport.getUser().getId())")
    @Mapping(target = "userEmail", expression = "java(bugReport.getUser().getEmail())")
    @Mapping(target = "statusId", expression = "java(bugReport.getStatus().getId())")
    @Mapping(target = "statusName", expression = "java(bugReport.getStatus().getName())")
    fun bugReportToBugReportResponse(bugReport: BugReport): BugReportResponse
    fun bugReportsListToBugReportResponsesList(bugReportsList: List<BugReport>): List<BugReportResponse>
}

package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportResponse
import sh4dow18.miteve_api.entities.SuggestedContentReport

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SuggestedContentReportMapper {
    @Mapping(target = "userId", expression = "java(report.getUser().getId())")
    @Mapping(target = "userEmail", expression = "java(report.getUser().getEmail())")
    @Mapping(target = "statusId", expression = "java(report.getStatus().getId())")
    @Mapping(target = "statusName", expression = "java(report.getStatus().getName())")
    fun suggestedContentReportToResponse(report: SuggestedContentReport): SuggestedContentReportResponse
    fun suggestedContentReportsListToResponsesList(list: List<SuggestedContentReport>): List<SuggestedContentReportResponse>
}

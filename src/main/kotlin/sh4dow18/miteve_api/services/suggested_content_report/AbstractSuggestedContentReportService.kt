package sh4dow18.miteve_api.services.suggested_content_report

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportRequest
import sh4dow18.miteve_api.dtos.suggested_content_report.SuggestedContentReportResponse
import sh4dow18.miteve_api.dtos.suggested_content_report.UpdateSuggestedContentReportStatusRequest
import sh4dow18.miteve_api.entities.SuggestedContentReport
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.SuggestedContentReportMapper
import sh4dow18.miteve_api.repositories.SuggestedContentReportRepository
import sh4dow18.miteve_api.repositories.SuggestedContentReportStatusRepository
import sh4dow18.miteve_api.repositories.UserRepository
import java.time.ZonedDateTime

@Service
class AbstractSuggestedContentReportService(
    @Autowired val reportRepository: SuggestedContentReportRepository,
    @Autowired val statusRepository: SuggestedContentReportStatusRepository,
    @Autowired val userRepository: UserRepository,
    @Autowired val mapper: SuggestedContentReportMapper
) : SuggestedContentReportService {

    override fun findAll(): List<SuggestedContentReportResponse> {
        return mapper.suggestedContentReportsListToResponsesList(reportRepository.findAllByOrderByReportedAtDesc())
    }

    override fun findById(id: Long): SuggestedContentReportResponse {
        val report = reportRepository.findById(id).orElseThrow {
            NoExists("$id", "SuggestedContentReport")
        }
        return mapper.suggestedContentReportToResponse(report)
    }

    override fun findAllByUserId(userId: Long): List<SuggestedContentReportResponse> {
        userRepository.findById(userId).orElseThrow {
            NoExists("$userId", "User")
        }
        return mapper.suggestedContentReportsListToResponsesList(reportRepository.findAllByUserId(userId))
    }

    @Transactional
    override fun insert(request: SuggestedContentReportRequest): SuggestedContentReportResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NoExists("${request.userId}", "User")
        }
        val status = statusRepository.findByName("Solicitado").orElseThrow {
            NoExists("Solicitado", "SuggestedContentReportStatus")
        }
        val report = reportRepository.save(
            SuggestedContentReport(
                id = 0,
                message = request.message,
                reportedAt = ZonedDateTime.now(),
                user = user,
                status = status,
                rejectionReason = null
            )
        )
        return mapper.suggestedContentReportToResponse(report)
    }

    @Transactional
    override fun updateStatus(id: Long, request: UpdateSuggestedContentReportStatusRequest): SuggestedContentReportResponse {
        val report = reportRepository.findById(id).orElseThrow {
            NoExists("$id", "SuggestedContentReport")
        }
        val status = statusRepository.findById(request.statusId).orElseThrow {
            NoExists("${request.statusId}", "SuggestedContentReportStatus")
        }
        report.status = status
        report.rejectionReason = if (status.name == "Reprobado") request.rejectionReason else null
        return mapper.suggestedContentReportToResponse(reportRepository.save(report))
    }
}

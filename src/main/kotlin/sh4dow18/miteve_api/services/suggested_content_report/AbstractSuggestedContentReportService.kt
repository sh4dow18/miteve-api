package sh4dow18.miteve_api.services.suggested_content_report

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
import sh4dow18.miteve_api.repositories.ContentTypeRepository
import sh4dow18.miteve_api.repositories.UserRepository
import java.time.ZonedDateTime

@Service
class AbstractSuggestedContentReportService(
    @Autowired val reportRepository: SuggestedContentReportRepository,
    @Autowired val statusRepository: SuggestedContentReportStatusRepository,
    @Autowired val contentTypeRepository: ContentTypeRepository,
    @Autowired val userRepository: UserRepository,
    @Autowired val mapper: SuggestedContentReportMapper
) : SuggestedContentReportService {

    private val rejectedNames = listOf("Reprobado", "Rechazado", "Rechazada")

    override fun findAll(page: Int, size: Int): Page<SuggestedContentReportResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"))
        return reportRepository.findActiveReports(rejectedNames, pageable).map { mapper.suggestedContentReportToResponse(it) }
    }

    override fun findRejected(page: Int, size: Int): Page<SuggestedContentReportResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"))
        return reportRepository.findByStatusNameIn(rejectedNames, pageable).map { mapper.suggestedContentReportToResponse(it) }
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
        val contentType = contentTypeRepository.findById(request.contentTypeId).orElseThrow {
            NoExists("${request.contentTypeId}", "ContentType")
        }
        val report = reportRepository.save(
            SuggestedContentReport(
                id = 0,
                message = request.message,
                reportedAt = ZonedDateTime.now(),
                user = user,
                status = status,
                tmdbId = request.tmdbId,
                contentType = contentType,
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
        report.rejectionReason = if (status.name in rejectedNames) request.rejectionReason else null
        return mapper.suggestedContentReportToResponse(reportRepository.save(report))
    }

    override fun existsByTmdbIdAndContentTypeId(tmdbId: Long, contentTypeId: Long): Boolean {
        return reportRepository.existsByTmdbIdAndContentTypeId(tmdbId, contentTypeId)
    }
}

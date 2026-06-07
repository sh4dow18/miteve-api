package sh4dow18.miteve_api.services.bug_report

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.bug_report.BugReportRequest
import sh4dow18.miteve_api.dtos.bug_report.BugReportResponse
import sh4dow18.miteve_api.dtos.bug_report.UpdateBugReportStatusRequest
import sh4dow18.miteve_api.entities.BugReport
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.BugReportMapper
import sh4dow18.miteve_api.repositories.BugReportRepository
import sh4dow18.miteve_api.repositories.BugReportStatusRepository
import sh4dow18.miteve_api.repositories.UserRepository
import java.time.ZonedDateTime

// Spring Abstract Bug Report Service
@Service
class AbstractBugReportService(
    @Autowired val bugReportRepository: BugReportRepository,
    @Autowired val bugReportStatusRepository: BugReportStatusRepository,
    @Autowired val userRepository: UserRepository,
    @Autowired val bugReportMapper: BugReportMapper
) : BugReportService {

    override fun findAll(): List<BugReportResponse> {
        return bugReportMapper.bugReportsListToBugReportResponsesList(bugReportRepository.findAllByOrderByReportedAtDesc())
    }

    override fun findById(id: Long): BugReportResponse {
        val bugReport = bugReportRepository.findById(id).orElseThrow {
            NoExists("$id", "BugReport")
        }
        return bugReportMapper.bugReportToBugReportResponse(bugReport)
    }

    override fun findAllByUserId(userId: Long): List<BugReportResponse> {
        userRepository.findById(userId).orElseThrow {
            NoExists("$userId", "User")
        }
        return bugReportMapper.bugReportsListToBugReportResponsesList(
            bugReportRepository.findAllByUserId(userId)
        )
    }

    @Transactional
    override fun insert(request: BugReportRequest): BugReportResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NoExists("${request.userId}", "User")
        }
        val status = bugReportStatusRepository.findByName("Reportado").orElseThrow {
            NoExists("Reportado", "BugReportStatus")
        }
        val bugReport = bugReportRepository.save(
            BugReport(
                id = 0,
                message = request.message,
                reportedAt = ZonedDateTime.now(),
                user = user,
                status = status
            )
        )
        return bugReportMapper.bugReportToBugReportResponse(bugReport)
    }

    @Transactional
    override fun updateStatus(id: Long, request: UpdateBugReportStatusRequest): BugReportResponse {
        val bugReport = bugReportRepository.findById(id).orElseThrow {
            NoExists("$id", "BugReport")
        }
        val status = bugReportStatusRepository.findById(request.statusId).orElseThrow {
            NoExists("${request.statusId}", "BugReportStatus")
        }
        bugReport.status = status
        return bugReportMapper.bugReportToBugReportResponse(bugReportRepository.save(bugReport))
    }
}

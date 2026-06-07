package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.BugReport

@Repository
interface BugReportRepository : JpaRepository<BugReport, Long> {
    fun findAllByOrderByReportedAtDesc(): List<BugReport>
    fun findAllByUserId(userId: Long): List<BugReport>
}

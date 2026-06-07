package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.BugReportStatus
import java.util.Optional

@Repository
interface BugReportStatusRepository : JpaRepository<BugReportStatus, Long> {
    fun findByName(name: String): Optional<BugReportStatus>
}

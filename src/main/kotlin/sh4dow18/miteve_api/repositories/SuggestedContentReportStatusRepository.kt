package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.SuggestedContentReportStatus
import java.util.Optional

@Repository
interface SuggestedContentReportStatusRepository : JpaRepository<SuggestedContentReportStatus, Long> {
    fun findByName(name: String): Optional<SuggestedContentReportStatus>
}

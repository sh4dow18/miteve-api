package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.SuggestedContentReport

@Repository
interface SuggestedContentReportRepository : JpaRepository<SuggestedContentReport, Long> {
    fun findAllByOrderByReportedAtDesc(): List<SuggestedContentReport>
    fun findAllByUserId(userId: Long): List<SuggestedContentReport>
}

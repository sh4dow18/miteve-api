package sh4dow18.miteve_api.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "suggested_content_reports")
class SuggestedContentReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @Column(length = 1000)
    var message: String,
    var reportedAt: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    var user: User,
    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false, referencedColumnName = "id")
    var status: SuggestedContentReportStatus,
    var tmdbId: Long,
    @Column(length = 1000, nullable = true)
    var rejectionReason: String? = null
)

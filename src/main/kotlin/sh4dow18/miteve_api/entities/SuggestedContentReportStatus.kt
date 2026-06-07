package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "suggested_content_report_statuses")
data class SuggestedContentReportStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @OneToMany(mappedBy = "status", targetEntity = SuggestedContentReport::class)
    var suggestedContentReportsList: List<SuggestedContentReport>
)

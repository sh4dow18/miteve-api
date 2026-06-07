package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "history")
class History(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false, referencedColumnName = "id")
    var content: Content,
    var time: Long,
    var viewedAt: ZonedDateTime,
    var viewCount: Int,
    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false, referencedColumnName = "id")
    var profile: Profile
)

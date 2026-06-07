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
@Table(name = "comments")
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @Column(length = 1000)
    var message: String,
    var rating: Int,
    var createdAt: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false, referencedColumnName = "id")
    var profile: Profile,
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false, referencedColumnName = "id")
    var content: Content
)

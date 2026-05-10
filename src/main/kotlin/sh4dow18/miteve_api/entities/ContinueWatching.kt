package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "profiles")
class ContinueWatching(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var time: Long,
    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false, referencedColumnName = "id")
    var profile: Profile,
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = true, referencedColumnName = "id")
    var content: Content?,
    @ManyToOne
    @JoinColumn(name = "episode_id", nullable = true, referencedColumnName = "id")
    var episode: Episode?
)
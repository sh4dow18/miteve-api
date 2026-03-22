package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table

// Season Entity
@Entity
@Table(name = "seasons")
data class Season(
    @Id
    var id: String,
    var seasonNumber: Int,
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false, referencedColumnName = "id")
    var content: Content,
    @OneToMany(mappedBy = "season", targetEntity = Episode::class)
    @OrderBy("episodeNumber ASC")
    var episodesList: List<Episode>
)
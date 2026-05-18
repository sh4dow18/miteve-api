package sh4dow18.miteve_api.entities

import jakarta.persistence.*

// Episode Entity
@Entity
@Table(name = "episodes")
data class Episode(
    @Id
    var id: String,
    var episodeNumber: Int,
    var title: String,
    var cover: String,
    @Column(length = 1000)
    var description: String,
    var beginSummary: Long?,
    var endSummary: Long?,
    var beginIntro: Long?,
    var endIntro: Long?,
    var beginCredits: Long?,
    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false, referencedColumnName = "id")
    var season: Season,
    @OneToMany(mappedBy = "episode", targetEntity = ContinueWatching::class)
    var continueWatchingList: List<ContinueWatching>,
)
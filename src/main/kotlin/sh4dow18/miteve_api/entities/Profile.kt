package sh4dow18.miteve_api.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "profiles")
class Profile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @Column(columnDefinition = "boolean default false")
    var autoSkip: Boolean = false,
    @Column(columnDefinition = "boolean default false")
    var lowQuality: Boolean = false,
    @Column(columnDefinition = "boolean default false")
    var disableSubtitles: Boolean = false,
    @Column(columnDefinition = "boolean default false")
    var adultProfile: Boolean = false,
    @Column(columnDefinition = "boolean default true")
    var allowPersonalizedRecommendations: Boolean = true,
    @OneToMany(mappedBy = "profile", targetEntity = ContinueWatching::class)
    var continueWatchingList: List<ContinueWatching>,
    @OneToMany(mappedBy = "profile", targetEntity = History::class)
    var historyList: List<History>,
    @ManyToMany(targetEntity = Content::class)
    @JoinTable(
        name = "profile_favorites",
        joinColumns = [JoinColumn(name = "profile_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "content_id", referencedColumnName = "id")]
    )
    var favoritesList: MutableSet<Content>,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    var user: User,
)
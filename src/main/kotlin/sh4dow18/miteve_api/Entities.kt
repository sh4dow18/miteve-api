package sh4dow18.miteve_api
// Entities Requirements
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
// Genre Entity
@Entity
@Table(name = "genres")
data class Genre(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(mappedBy = "genresList", fetch = FetchType.LAZY, targetEntity = Movie::class)
    var moviesList: Set<Movie>,
    @ManyToMany(mappedBy = "genresList", fetch = FetchType.LAZY, targetEntity = Series::class)
    var seriesList: Set<Series>
) {
    override fun equals(other: Any?): Boolean {
        // Check if the current object is the same instance as other
        if (this === other) return true
        // Check if other is a Role
        if (other !is Genre) return false
        // Compare the id of this object with the id of the other object
        return id == other.id
    }
    // Use the hashCode of the "id" field as the hash code for the entire object
    override fun hashCode(): Int = id.hashCode()
}
// Movie Entity
@Entity
@Table(name = "movies")
data class Movie(
    @Id
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String?,
    @Column(length = 1000)
    var description: String,
    var rating: Float,
    var classification: String?,
    @Column(name = "movie_cast")
    var cast: String?,
    var collection: String?,
    var company: String,
    var cover: String,
    var background: String,
    var trailer: String,
    var content: String,
    @ManyToMany(targetEntity = Genre::class)
    @JoinTable(
        name = "movie_genre",
        joinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")]
    )
    var genresList: Set<Genre>,
    @ManyToMany(mappedBy = "moviesList", fetch = FetchType.LAZY, targetEntity = Profile::class)
    var profilesList: MutableSet<Profile>,
) {
    override fun equals(other: Any?): Boolean {
        // Check if the current object is the same instance as other
        if (this === other) return true
        // Check if other is a Role
        if (other !is Movie) return false
        // Compare the id of this object with the id of the other object
        return id == other.id
    }
    // Use the hashCode of the "id" field as the hash code for the entire object
    override fun hashCode(): Int = id.hashCode()
}
// Series Entity
@Entity
@Table(name = "series")
data class Series(
    @Id
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String,
    @Column(length = 1000)
    var description: String,
    var rating: Int,
    var classification: String,
    @Column(name = "series_cast")
    var cast: String,
    var originCountry: String,
    var cover: String,
    var background: String,
    var trailer: String,
    @ManyToMany(targetEntity = Genre::class)
    @JoinTable(
        name = "series_genre",
        joinColumns = [JoinColumn(name = "series_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")]
    )
    var genresList: Set<Genre>,
    @OneToMany(mappedBy = "series", targetEntity = Season::class)
    var seasonsList: List<Season>,
    @ManyToMany(mappedBy = "seriesList", fetch = FetchType.LAZY, targetEntity = Profile::class)
    var profilesList: MutableSet<Profile>,
)
// Season Entity
@Entity
@Table(name = "seasons")
data class Season(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var seasonNumber: Int,
    @ManyToOne
    @JoinColumn(name = "series_id", nullable = false, referencedColumnName = "id")
    var series: Series,
    @OneToMany(mappedBy = "season", targetEntity = Episode::class)
    var episodesList: List<Episode>
)
// Episode Entity
@Entity
@Table(name = "episodes")
data class Episode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var episodeNumber: Int,
    var title: String,
    var cover: String,
    @Column(length = 1000)
    var description: String,
    var beginIntro: Long?,
    var endIntro: Long?,
    var beginCredits: Long?,
    var endCredits: Long?,
    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false, referencedColumnName = "id")
    var season: Season
)
// Privilege Entity
@Entity
@Table(name = "privileges")
data class Privilege(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(targetEntity = Role::class)
    @JoinTable(
        name = "privilege_role",
        joinColumns = [JoinColumn(name = "privilege_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")]
    )
    var rolesList: MutableSet<Role>,
)
// Role Entity
@Entity
@Table(name = "roles")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(mappedBy = "rolesList", fetch = FetchType.LAZY, targetEntity = Privilege::class)
    var privilegesList: MutableSet<Privilege>,
    @ManyToMany(targetEntity = User::class)
    @JoinTable(
        name = "role_user",
        joinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")]
    )
    var usersList: MutableSet<User>,
)
// User Entity
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var email: String,
    var password: String,
    @ManyToMany(mappedBy = "usersList", fetch = FetchType.LAZY, targetEntity = Role::class)
    var rolesList: MutableSet<Role>,
    @OneToMany(mappedBy = "user", targetEntity = Profile::class)
    var profilesList: List<Profile>,
)
// Profile Entity
@Entity
@Table(name = "profiles")
data class Profile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(targetEntity = Movie::class)
    @JoinTable(
        name = "profile_movie",
        joinColumns = [JoinColumn(name = "profile_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")]
    )
    var moviesList: MutableSet<Movie>,
    @ManyToMany(targetEntity = Series::class)
    @JoinTable(
        name = "profile_series",
        joinColumns = [JoinColumn(name = "profile_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "series_id", referencedColumnName = "id")]
    )
    var seriesList: MutableSet<Series>,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    var user: User
)
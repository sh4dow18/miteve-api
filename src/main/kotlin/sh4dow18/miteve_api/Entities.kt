package sh4dow18.miteve_api

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

@Entity
@Table(name = "genres")
data class Genre(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(targetEntity = Movie::class)
    @JoinTable(
        name = "genre_movie",
        joinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")]
    )
    var moviesList: MutableSet<Movie>,
    @ManyToMany(targetEntity = Series::class)
    @JoinTable(
        name = "genre_series",
        joinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "series_id", referencedColumnName = "id")]
    )
    var seriesList: MutableSet<Series>
)

@Entity
@Table(name = "movies")
data class Movie(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var tmdbId: Long,
    var title: String,
    var tagline: String,
    var description: String,
    var rating: Int,
    var classification: String,
    @Column(name = "movie_cast")
    var cast: String,
    var cover: String,
    var background: String,
    var trailer: String,
    var content: String,
    @ManyToMany(mappedBy = "moviesList", fetch = FetchType.LAZY, targetEntity = Genre::class)
    var genresList: MutableSet<Genre>,
    @ManyToMany(mappedBy = "moviesList", fetch = FetchType.LAZY, targetEntity = Profile::class)
    var profilesList: MutableSet<Profile>,
)

@Entity
@Table(name = "series")
data class Series(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var tmdbId: Long,
    var title: String,
    var tagline: String,
    var description: String,
    var rating: Int,
    var classification: String,
    @Column(name = "series_cast")
    var cast: String,
    var cover: String,
    var background: String,
    var trailer: String,
    @ManyToMany(mappedBy = "seriesList", fetch = FetchType.LAZY, targetEntity = Genre::class)
    var genresList: MutableSet<Genre>,
    @OneToMany(mappedBy = "series", targetEntity = Season::class)
    var seasonsList: List<Season>,
    @ManyToMany(mappedBy = "seriesList", fetch = FetchType.LAZY, targetEntity = Profile::class)
    var profilesList: MutableSet<Profile>,
)

@Entity
@Table(name = "series")
data class Season(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var number: Int,
    @ManyToOne
    @JoinColumn(name = "series_id", nullable = false, referencedColumnName = "id")
    var series: Series,
    @OneToMany(mappedBy = "season", targetEntity = Episode::class)
    var episodesList: List<Episode>
)

@Entity
@Table(name = "series")
data class Episode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var number: Int,
    var description: String,
    var beginIntro: Long,
    var endIntro: Long,
    var beginCredits: Long,
    var endCredits: Long,
    var content: String,
    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false, referencedColumnName = "id")
    var season: Season
)

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
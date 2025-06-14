package sh4dow18.miteve_api

// Requests

data class GenreRequest(
    var name: String,
)
data class MovieRequest(
    var tmdbId: Long?,
    var title: String,
    var tagline: String?,
    var description: String,
    var rating: Float,
    var classification: String?,
    var cast: String?,
    var cover: String,
    var background: String,
    var trailer: String,
    var content: String,
    var genresList: Set<Long>
)

// Responses
data class GenreResponse(
    var id: Long,
    var name: String,
)
data class MovieResponse(
    var id: Long,
    var tmdbId: Long?,
    var title: String,
    var tagline: String?,
    var description: String,
    var rating: Float,
    var classification: String?,
    var cast: String?,
    var cover: String,
    var background: String,
    var trailer: String,
    var content: String,
    var genres: String
)
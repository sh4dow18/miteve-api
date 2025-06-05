package sh4dow18.miteve_api

// Requests

data class GenreRequest(
    var name: String,
)
data class MovieRequest(
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String?,
    var description: String,
    var rating: Float,
    var classification: String?,
    var cast: String?,
    var company: String,
    var collection: String?,
    var cover: String,
    var background: String,
    var trailer: String,
    var content: String,
    var genresList: Set<Long>
)
data class SeriesRequest(
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String,
    var description: String,
    var rating: Int,
    var classification: String,
    var cast: String,
    var originCountry: String,
    var cover: String,
    var background: String,
    var trailer: String,
    var genresList: Set<Long>,
)
data class SeasonRequest(
    var seasonNumber: Int,
    var episodesList: List<EpisodeRequest>
)
data class EpisodeRequest(
    var episodeNumber: Int,
    var title: String,
    var description: String
)

// Responses
data class GenreResponse(
    var id: Long,
    var name: String,
)
data class MovieResponse(
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String?,
    var description: String,
    var rating: Float,
    var classification: String?,
    var cast: String?,
    var cover: String,
    var background: String,
    var trailer: String,
    var genres: String
)
data class SeriesResponse(
    var id: Long,
    var title: String,
    var year: String,
    var tagline: String,
    var description: String,
    var rating: Int,
    var classification: String,
    var cast: String,
    var originCountry: String,
    var cover: String,
    var background: String,
    var trailer: String,
    var genres: String,
    var seasonsList: List<Int>
)
data class InsertEpisodesResponse(
    var id: Long,
    var seasonsList: List<Int>
)
data class SeasonResponse(
    var id: Long,
    var seasonNumber: Int,
    var episodesList: List<EpisodeResponse>
)
data class EpisodeResponse(
    var id: Long,
    var episodeNumber: Int,
    var title: String,
    var description: String
)
data class NextEpisodeResponse(
    var id: Long,
    var seasonNumber: Int,
    var episodeNumber: Int,
)

// Minimal Responses

data class MinimalMovieResponse(
    var id: Long,
    var title: String,
    var cover: String,
)
data class MinimalSeriesResponse(
    var id: Long,
    var title: String,
    var cover: String,
)
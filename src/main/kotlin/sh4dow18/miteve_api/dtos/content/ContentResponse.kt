package sh4dow18.miteve_api.dtos.content

import sh4dow18.miteve_api.dtos.genre.GenreResponse
import sh4dow18.miteve_api.dtos.season.SeasonResponse
import java.time.ZonedDateTime

data class ContentResponse(
    var id: String,
    var title: String,
    var year: Short,
    var tagline: String?,
    var description: String,
    var rating: Float,
    var age: Short,
    var cover: String,
    var background: String,
    var trailer: String,
    var trailerDuration: Int,
    var comingSoon: Boolean,
    var createdDate: ZonedDateTime,
    var note: String?,
    var type: String,
    var genresList: List<GenreResponse>,
    var seasonsList: List<SeasonResponse>,
)
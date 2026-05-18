package sh4dow18.miteve_api.dtos.content

data class ContentRequest(
    var tmdbId: Long,
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
    var note: String?,
    var typeId: Long,
    var genresList: List<Long>,
    var containerId: Long,
    var containerPosition: Short
)

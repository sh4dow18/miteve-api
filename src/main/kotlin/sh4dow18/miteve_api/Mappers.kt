package sh4dow18.miteve_api
// Mappers Requirements
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
// Mappers Constants
const val EMPTY_SET = "java(java.util.Collections.emptySet())"
const val EMPTY_LIST = "java(java.util.Collections.emptyList())"
// Genre Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface GenreMapper {
    // Set each set as empty
    @Mapping(target = "moviesList", expression = EMPTY_SET)
    @Mapping(target = "seriesList", expression = EMPTY_SET)
    fun genreRequestToGenre(
        genreRequest: GenreRequest
    ): Genre
    fun genreToGenreResponse(
        genre: Genre
    ): GenreResponse
    fun genresListToGenreResponsesList(
        genresList: List<Genre>
    ): List<GenreResponse>
}
// Movie Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface MovieMapper {
    // Set genres list as the genres list sent
    @Mapping(target = "genresList", expression = "java(genresList)")
    // Set each set as empty
    @Mapping(target = "profilesList", expression = EMPTY_SET)
    fun movieRequestToMovie(
        movieRequest: MovieRequest,
        genresList: Set<Genre>
    ): Movie
    // Transform genres list from list tro string with this format "Element 1, Element 2, etc."
    @Mapping(target = "genres", expression = "java(movie.getGenresList().stream().map(Genre::getName).collect(java.util.stream.Collectors.joining(\", \")))")
    fun movieToMovieResponse(
        movie: Movie
    ): MovieResponse
    fun movieToMinimalMovieResponse(
        movie: Movie
    ): MinimalMovieResponse
    fun moviesListToMinimalMovieResponsesList(
        moviesList: List<Movie>
    ): List<MinimalMovieResponse>
}
// Series Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SeriesMapper {
    // Set genres list as the genres list sent
    @Mapping(target = "genresList", expression = "java(genresList)")
    // Set each set as empty
    @Mapping(target = "seasonsList", expression = EMPTY_LIST)
    @Mapping(target = "profilesList", expression = EMPTY_SET)
    fun seriesRequestToSeries(
        seriesRequest: SeriesRequest,
        genresList: Set<Genre>
    ): Series
    // Transform genres list from list tro string with this format "Element 1, Element 2, etc."
    @Mapping(target = "genres", expression = "java(series.getGenresList().stream().map(Genre::getName).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "seasonsList", expression = "java(series.getSeasonsList().stream().map(Season::getSeasonNumber).collect(java.util.stream.Collectors.toList()))")
    fun seriesToSeriesResponse(
        series: Series
    ): SeriesResponse
    fun seriesToMinimalSeriesResponse(
        series: Series
    ): MinimalSeriesResponse
    fun seriesListToMinimalSeriesResponsesList(
        seriesList: List<Series>
    ): List<MinimalSeriesResponse>
}
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SeasonMapper {
    // Set each set as empty
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "episodesList", expression = EMPTY_LIST)
    @Mapping(target = "series", expression = "java(newSeries)")
    fun seasonRequestToSeason(
        seasonRequest: SeasonRequest,
        newSeries: Series
    ): Season
    fun seasonToSeasonResponse(
        season: Season
    ): SeasonResponse
}
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EpisodeMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "season", expression = "java(newSeason)")
    fun episodeRequestToEpisode(
        episodeRequest: EpisodeRequest,
        newSeason: Season
    ): Episode
    fun episodeToEpisodeResponse(
        episode: Episode
    ): EpisodeResponse
}

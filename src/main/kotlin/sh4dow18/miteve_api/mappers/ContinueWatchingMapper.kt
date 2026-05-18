package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.entities.ContinueWatching

// ContinueWatching Mapper
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = [ContentMapper::class, EpisodeMapper::class]
)
interface ContinueWatchingMapper {
    fun continueWatchingToContinueWatchingResponse(continueWatching: ContinueWatching): ContinueWatchingResponse
    fun continueWatchingListToContinueWatchingResponsesList(
        continueWatchingList: List<ContinueWatching>
    ): List<ContinueWatchingResponse>
}


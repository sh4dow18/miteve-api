package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.history.HistoryResponse
import sh4dow18.miteve_api.entities.History

// History Mapper
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = [ContentMapper::class]
)
interface HistoryMapper {
    fun historyToHistoryResponse(history: History): HistoryResponse
    fun historyListToHistoryResponseList(historyList: List<History>): List<HistoryResponse>
}

package sh4dow18.miteve_api.services.history

import sh4dow18.miteve_api.dtos.history.HistoryResponse

// History Service Interface where the functions to be used in
// Spring Abstract History Service are declared
interface HistoryService {
    fun findAllByProfileId(profileId: Long): List<HistoryResponse>
}

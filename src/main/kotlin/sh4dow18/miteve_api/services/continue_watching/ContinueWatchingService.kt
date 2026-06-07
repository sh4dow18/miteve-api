package sh4dow18.miteve_api.services.continue_watching

import sh4dow18.miteve_api.dtos.continue_watching.AddContinueWatchingRequest
import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse
import sh4dow18.miteve_api.dtos.continue_watching.UpdateContinueWatchingTimeRequest

// ContinueWatching Service Interface where the functions to be used in
// Spring Abstract ContinueWatching Service are declared
interface ContinueWatchingService {
    fun updateTime(id: Long, request: UpdateContinueWatchingTimeRequest): ContinueWatchingResponse
    fun addOrUpdate(request: AddContinueWatchingRequest): ContinueWatchingResponse
    fun delete(id: Long)
}


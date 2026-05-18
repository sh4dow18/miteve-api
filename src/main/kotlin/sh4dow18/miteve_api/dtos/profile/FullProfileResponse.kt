package sh4dow18.miteve_api.dtos.profile

import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse

data class FullProfileResponse(
    var id: Long,
    var name: String,
    var continueWatchingList: List<ContinueWatchingResponse>,
)


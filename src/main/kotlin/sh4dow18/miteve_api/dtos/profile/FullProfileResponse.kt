package sh4dow18.miteve_api.dtos.profile

import sh4dow18.miteve_api.dtos.continue_watching.ContinueWatchingResponse

data class FullProfileResponse(
    var id: Long,
    var name: String,
    var autoSkip: Boolean,
    var lowQuality: Boolean,
    var disableSubtitles: Boolean,
    var adultProfile: Boolean,
    var allowPersonalizedRecommendations: Boolean,
    var continueWatchingList: List<ContinueWatchingResponse>,
)


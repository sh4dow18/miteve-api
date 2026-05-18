package sh4dow18.miteve_api.dtos.continue_watching

data class AddContinueWatchingRequest(
    var profileId: Long,
    var contentId: String?,
    var episodeId: String?,
    var time: Long,
)


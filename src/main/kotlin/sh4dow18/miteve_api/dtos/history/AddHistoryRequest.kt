package sh4dow18.miteve_api.dtos.history

data class AddHistoryRequest(
    var profileId: Long,
    var contentId: String,
    var time: Long,
)

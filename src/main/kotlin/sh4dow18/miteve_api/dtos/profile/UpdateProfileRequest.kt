package sh4dow18.miteve_api.dtos.profile

data class UpdateProfileRequest(
    val name: String? = null,
    val autoSkip: Boolean? = null,
    val lowQuality: Boolean? = null,
    val disableSubtitles: Boolean? = null,
    val adultProfile: Boolean? = null,
    val allowPersonalizedRecommendations: Boolean? = null
)

package sh4dow18.miteve_api.services.season

import sh4dow18.miteve_api.dtos.episode.FullEpisodeResponse

// Genre Service Interface where the functions to be used in
// Spring Abstract Content Genre are declared
interface SeasonService {
    fun findEpisodesById(id: String): List<FullEpisodeResponse>
}
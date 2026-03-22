package sh4dow18.miteve_api.services.episode

import sh4dow18.miteve_api.dtos.episode.EpisodeMetadataResponse
import sh4dow18.miteve_api.dtos.episode.NextEpisodeResponse

// Episode Service Interface where the functions to be used in
// Spring Abstract Content Episode are declared
interface EpisodeService {
    fun findNextById(id: String): NextEpisodeResponse
    fun findMetadataById(id: String): EpisodeMetadataResponse
}
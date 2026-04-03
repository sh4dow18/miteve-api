package sh4dow18.miteve_api.services.content

import sh4dow18.miteve_api.dtos.content.ContentRequest
import sh4dow18.miteve_api.dtos.content.ContentResponse
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.content.ShortContentResponse
import sh4dow18.miteve_api.dtos.episode.InsertEpisodesResponse
import sh4dow18.miteve_api.dtos.season.MiniSeasonResponse
import sh4dow18.miteve_api.dtos.season.SeasonRequest

// Content Service Interface where the functions to be used in
// Spring Abstract Content Service are declared
interface ContentService {
    fun findAll(): List<ShortContentResponse>
    fun findById(id: String): ContentResponse
    fun findRecentContent(): List<MiniContentResponse>
    fun findComingSoon(): List<MiniContentResponse>
    fun findSeasonsById(id: String): List<MiniSeasonResponse>
    fun insert(contentRequest: ContentRequest): ContentResponse
    fun insertEpisodes(id: String, seasonsList: List<SeasonRequest>): InsertEpisodesResponse
    fun update(id: String, contentRequest: ContentRequest): ContentResponse
}
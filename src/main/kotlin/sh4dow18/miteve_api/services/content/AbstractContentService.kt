package sh4dow18.miteve_api.services.content

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.content.ContentRequest
import sh4dow18.miteve_api.dtos.content.ContentResponse
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.content.ShortContentResponse
import sh4dow18.miteve_api.dtos.episode.InsertEpisodesResponse
import sh4dow18.miteve_api.dtos.season.MiniSeasonResponse
import sh4dow18.miteve_api.dtos.season.SeasonRequest
import sh4dow18.miteve_api.errors.AlreadyExists
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContainerElementMapper
import sh4dow18.miteve_api.mappers.ContentMapper
import sh4dow18.miteve_api.mappers.EpisodeMapper
import sh4dow18.miteve_api.mappers.SeasonMapper
import sh4dow18.miteve_api.repositories.*
import java.text.Normalizer

// Spring Abstract Content Service
@Service
class AbstractContentService(
    @Autowired
    val contentRepository: ContentRepository,
    @Autowired
    val genreRepository: GenreRepository,
    @Autowired
    val containerRepository: ContainerRepository,
    @Autowired
    val containerElementRepository: ContainerElementRepository,
    @Autowired
    val contentTypeRepository: ContentTypeRepository,
    @Autowired
    val seasonRepository: SeasonRepository,
    @Autowired
    val episodeRepository: EpisodeRepository,
    @Autowired
    val contentMapper: ContentMapper,
    @Autowired
    val containerElementMapper: ContainerElementMapper,
    @Autowired
    val seasonMapper: SeasonMapper,
    @Autowired
    val episodeMapper: EpisodeMapper
): ContentService {
    fun toSlug(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "") // quita acentos
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
    override fun findAll(): List<ShortContentResponse> {
        return contentMapper.contentsListToShortContentResponsesList(contentRepository.findAll())
    }
    override fun findById(id: String): ContentResponse {
        val content = contentRepository.findById(id).orElseThrow {
            NoExists(id, "Content")
        }
        return contentMapper.contentToContentResponse(content)
    }
    override fun findRecentContent(): List<MiniContentResponse> {
        return contentMapper.contentsListToMiniContentResponsesList(contentRepository.findTop10ByComingSoonFalseOrderByCreatedDateDesc())
    }
    override fun findComingSoon(): List<MiniContentResponse> {
        return contentMapper.contentsListToMiniContentResponsesList(contentRepository.findByComingSoonTrueOrderByCreatedDateDesc())
    }
    override fun findSeasonsById(id: String): List<MiniSeasonResponse> {
        val content = contentRepository.findById(id).orElseThrow {
            NoExists(id, "Content")
        }
        return seasonMapper.seasonsListToMiniSeasonResponsesList(content.seasonsList)
    }
    @Transactional
    override fun insert(contentRequest: ContentRequest): ContentResponse {
        val slug = toSlug(contentRequest.title)
        if (contentRepository.findById(slug).orElse(null) != null) {
            throw AlreadyExists(contentRequest.title, "Content")
        }
        val genresList = genreRepository.findAllById(contentRequest.genresList)
        if (genresList.size != contentRequest.genresList.size) {
            val missingIds = contentRequest.genresList - genresList.map { it.id }.toSet()
            throw NoExists(missingIds.toList().toString(), "Genres")
        }
        val container = containerRepository.findById(contentRequest.containerId).orElseThrow {
            NoExists("${contentRequest.containerId}", "Container")
        }
        val type = contentTypeRepository.findById(contentRequest.typeId).orElseThrow {
            NoExists("${contentRequest.typeId}", "Content Type")
        }
        containerElementRepository.shiftPositionFrom(container, contentRequest.containerPosition)
        val newContent = contentRepository.save(contentMapper.contentRequestToContent(slug, contentRequest, genresList.toSet(), type))
        val newContainerElement = containerElementMapper.contentElementRequestToContainerElement(contentRequest.containerPosition, newContent, container)
        containerElementRepository.save(newContainerElement)
        return contentMapper.contentToContentResponse(newContent)
    }
    @Transactional
    override fun insertEpisodes(id: String, seasonsList: List<SeasonRequest>): InsertEpisodesResponse {
        val content = contentRepository.findById(id).orElseThrow {
            NoExists(id, "Content")
        }
        seasonsList.forEach { seasonRequest ->
            // Check if exists already the season submitted
            val existingSeason = content.seasonsList.find { it.seasonNumber == seasonRequest.seasonNumber }
            // Set Season Id
            val seasonId = content.id + "-" + seasonRequest.seasonNumber
            // If the season exists, set the existing season, if not, create a new one
            val season = existingSeason ?: seasonRepository.save(seasonMapper.seasonRequestToSeason(seasonId, seasonRequest, content))
            seasonRequest.episodesList.forEach { episodeRequest ->
                // Check if exists already the episode submitted from the season submitted
                val existingEpisode = season.episodesList.find { it.episodeNumber == episodeRequest.episodeNumber }
                // Set Epísode Id
                val episodeId = seasonId + "-" + episodeRequest.episodeNumber
                // If the episode does not exist, create a new one
                if (existingEpisode == null) {
                    episodeRepository.save(episodeMapper.episodeRequestToEpisode(episodeId, episodeRequest, season))
                }
            }
        }
        return InsertEpisodesResponse(id = content.id, seasonsList = content.seasonsList.map { it.seasonNumber })
    }
    @Transactional
    override fun update(id: String, contentRequest: ContentRequest): ContentResponse {
        val existingContentData = contentRepository.findById(id).orElseThrow {
            NoExists(id, "Content")
        }
        val genresList = genreRepository.findAllById(contentRequest.genresList)
        if (genresList.size != contentRequest.genresList.size) {
            val missingIds = contentRequest.genresList - genresList.map { it.id }.toSet()
            throw NoExists(missingIds.toList().toString(), "Genres")
        }
        val container = containerRepository.findById(contentRequest.containerId).orElseThrow {
            NoExists("${contentRequest.containerId}", "Container")
        }
        val type = contentTypeRepository.findById(contentRequest.typeId).orElseThrow {
            NoExists("${contentRequest.typeId}", "Content Type")
        }
        containerElementRepository.shiftPositionFrom(container, contentRequest.containerPosition)
        val content = contentMapper.contentRequestToContent(id, contentRequest, genresList.toSet(), type)
        if (existingContentData.containerElement != null) {
            content.containerElement = existingContentData.containerElement
            content.containerElement!!.position = contentRequest.containerPosition
            content.containerElement!!.container = container
        }
        return contentMapper.contentToContentResponse(contentRepository.save(content))
    }
}
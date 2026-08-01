package sh4dow18.miteve_api.services.batch_update

import tools.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import sh4dow18.miteve_api.dtos.batch_update.BatchUpdateEpisodesResponse
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.repositories.ContentRepository
import sh4dow18.miteve_api.repositories.EpisodeRepository
import javax.xml.parsers.DocumentBuilderFactory

@Service
class AbstractBatchUpdateEpisodesService(
    @Autowired val contentRepository: ContentRepository,
    @Autowired val episodeRepository: EpisodeRepository
) : BatchUpdateEpisodesService {

    private val restTemplate = RestTemplate()
    private val mpdBaseUrl = "https://miteve.ddnsfree.com/stream"

    @Transactional
    override fun update(node: JsonNode): BatchUpdateEpisodesResponse {
        val seriesId = node.get("seriesId")?.asText()
            ?: throw BadRequest("seriesId is required")
        val startSeason = node.get("startSeason")?.intValue()
            ?: throw BadRequest("startSeason is required")
        val startEpisode = node.get("startEpisode")?.intValue()
            ?: throw BadRequest("startEpisode is required")
        val endSeason = node.get("endSeason")?.intValue()
            ?: throw BadRequest("endSeason is required")
        val endEpisode = node.get("endEpisode")?.intValue()
            ?: throw BadRequest("endEpisode is required")
        val endingDuration = node.get("endingDuration")?.longValue()
            ?: throw BadRequest("endingDuration is required")

        val hasBeginSummary = node.has("beginSummary")
        val hasEndSummary = node.has("endSummary")
        val hasBeginIntro = node.has("beginIntro")
        val hasEndIntro = node.has("endIntro")

        val beginSummary = if (hasBeginSummary && !node.get("beginSummary").isNull) node.get("beginSummary").longValue() else null
        val endSummary = if (hasEndSummary && !node.get("endSummary").isNull) node.get("endSummary").longValue() else null
        val beginIntro = if (hasBeginIntro && !node.get("beginIntro").isNull) node.get("beginIntro").longValue() else null
        val endIntro = if (hasEndIntro && !node.get("endIntro").isNull) node.get("endIntro").longValue() else null

        val content = contentRepository.findById(seriesId).orElseThrow {
            NoExists(seriesId, "Content")
        }

        if (startSeason > endSeason) {
            throw BadRequest("startSeason cannot be greater than endSeason")
        }
        if (startSeason == endSeason && startEpisode > endEpisode) {
            throw BadRequest("startEpisode cannot be greater than endEpisode when seasons are equal")
        }
        if (endingDuration < 0) {
            throw BadRequest("endingDuration cannot be negative")
        }

        val errors = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        for (seasonNum in startSeason..endSeason) {
            val seasonId = "${content.id}-$seasonNum"
            val season = content.seasonsList.find { it.seasonNumber == seasonNum }
            if (season == null) {
                val epStart = if (seasonNum == startSeason) startEpisode else 1
                val epEnd = if (seasonNum == endSeason) endEpisode else endEpisode
                val ep = epEnd - epStart + 1
                failureCount += ep
                repeat(ep) { i ->
                    errors.add("Season $seasonNum does not exist (episode ${startEpisode + i})")
                }
                continue
            }

            val episodeStart = if (seasonNum == startSeason) startEpisode else 1
            val episodeEnd = if (seasonNum == endSeason) endEpisode else {
                season.episodesList.maxOfOrNull { it.episodeNumber } ?: endEpisode
            }

            for (episodeNum in episodeStart..episodeEnd) {
                val episodeId = "$seasonId-$episodeNum"
                val episode = season.episodesList.find { it.episodeNumber == episodeNum }
                if (episode == null) {
                    failureCount++
                    errors.add("Episode $episodeId does not exist")
                    continue
                }

                try {
                    val mpdUrl = "$mpdBaseUrl/${content.id}/season-$seasonNum/episode-$episodeNum/manifest.mpd"
                    val durationInSeconds = fetchDurationFromMpd(mpdUrl)

                    if (durationInSeconds <= 0) {
                        failureCount++
                        errors.add("Could not obtain duration for episode $episodeId")
                        continue
                    }

                    val beginCredits = durationInSeconds - endingDuration

                    if (hasBeginSummary) episode.beginSummary = beginSummary
                    if (hasEndSummary) episode.endSummary = endSummary
                    if (hasBeginIntro) episode.beginIntro = beginIntro
                    if (hasEndIntro) episode.endIntro = endIntro
                    episode.beginCredits = beginCredits

                    episodeRepository.save(episode)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                    errors.add("Error processing episode $episodeId: ${e.message}")
                }
            }
        }

        return BatchUpdateEpisodesResponse(
            seriesId = content.id,
            totalProcessed = successCount + failureCount,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    private fun fetchDurationFromMpd(url: String): Long {
        val xmlBody = restTemplate.getForObject(url, String::class.java)
            ?: throw BadRequest("Could not fetch MPD from $url")

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlBody.byteInputStream())
        doc.documentElement.normalize()

        val root = doc.documentElement
        val mediaPresentationDuration = root.getAttribute("mediaPresentationDuration")
        if (mediaPresentationDuration.isNotBlank()) {
            return parseIso8601Duration(mediaPresentationDuration)
        }

        val segmentTemplate = doc.getElementsByTagName("SegmentTemplate")
        if (segmentTemplate.length > 0) {
            val totalDuration = segmentTemplate.item(0).attributes.getNamedItem("totalDuration")
            if (totalDuration != null) {
                return totalDuration.nodeValue.toDouble().toLong()
            }
        }

        throw BadRequest("Could not find duration in MPD manifest")
    }

    private fun parseIso8601Duration(duration: String): Long {
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?")
        val match = regex.matchEntire(duration)
            ?: throw BadRequest("Invalid duration format: $duration")

        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLongOrNull() ?: 0L
        val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0

        return hours * 3600 + minutes * 60 + seconds.toLong()
    }
}

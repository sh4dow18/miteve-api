package sh4dow18.miteve_api.services.batch_update

import tools.jackson.databind.JsonNode
import sh4dow18.miteve_api.dtos.batch_update.BatchUpdateEpisodesResponse

interface BatchUpdateEpisodesService {
    fun update(node: JsonNode): BatchUpdateEpisodesResponse
}

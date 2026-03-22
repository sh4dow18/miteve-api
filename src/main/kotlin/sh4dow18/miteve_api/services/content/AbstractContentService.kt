package sh4dow18.miteve_api.services.content

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.content.ContentResponse
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.ContentMapper
import sh4dow18.miteve_api.repositories.ContentRepository

// Spring Abstract Content Service
@Service
class AbstractContentService(
    @Autowired
    val contentRepository: ContentRepository,
    @Autowired
    val contentMapper: ContentMapper,
): ContentService {
    override fun findById(id: String): ContentResponse {
        val content = contentRepository.findById(id).orElseThrow {
            NoExists(id, "Content")
        }
        return contentMapper.contentToContentResponse(content)
    }
}
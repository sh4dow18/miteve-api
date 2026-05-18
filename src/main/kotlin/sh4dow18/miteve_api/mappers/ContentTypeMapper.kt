package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.content_type.ContentTypeResponse
import sh4dow18.miteve_api.entities.ContentType

// Content Type Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContentTypeMapper {
    fun contentTypesListToContentTypeResponsesList(
        contentTypesList: List<ContentType>
    ): List<ContentTypeResponse>
}
package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.content.ContentResponse
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.entities.Content

// Content Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContentMapper {
    @Mapping(target = "type", expression = "java(content.getType().getName())")
    fun contentToContentResponse(
        content: Content
    ): ContentResponse
    fun contentsListToContentResponsesList(
        contentsList: List<Content>
    ): List<ContentResponse>
    fun contentsListToMiniContentResponsesList(
        contentsList: List<Content>
    ): List<MiniContentResponse>
}
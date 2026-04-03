package sh4dow18.miteve_api.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import sh4dow18.miteve_api.dtos.content.ContentRequest
import sh4dow18.miteve_api.dtos.content.ContentResponse
import sh4dow18.miteve_api.dtos.content.MiniContentResponse
import sh4dow18.miteve_api.dtos.content.ShortContentResponse
import sh4dow18.miteve_api.entities.Content
import sh4dow18.miteve_api.entities.ContentType
import sh4dow18.miteve_api.entities.Genre

// Content Mapper
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = [ContainerMapper::class])
interface ContentMapper {
    // Set genres list as the genres list sent
    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "genresList", expression = "java(genresList)")
    @Mapping(target = "type", expression = "java(type)")
    @Mapping(target = "seasonsList", expression = EMPTY_LIST)
    // Set added at right now
    @Mapping(target = "createdDate", expression = "java(java.time.ZonedDateTime.now())")
    fun contentRequestToContent(
        id: String,
        contentRequest: ContentRequest,
        genresList: Set<Genre>,
        type: ContentType
    ): Content
    @Mapping(target = "type", expression = "java(content.getType().getName())")
    @Mapping(target = "position", expression = "java(content.getContainerElement() != null ? content.getContainerElement().getPosition() : null)")
    @Mapping(target = "container", source = "containerElement.container")
    fun contentToContentResponse(
        content: Content
    ): ContentResponse
    @Mapping(target = "type", expression = "java(content.getType().getName())")
    @Mapping(target = "createdDate", expression = "java(content.getCreatedDate().format(java.time.format.DateTimeFormatter.ofPattern(\"dd/MM/yyyy HH:mm\")))")
    fun contentToShortContentResponse(
        content: Content
    ): ShortContentResponse
    fun contentsListToContentResponsesList(
        contentsList: List<Content>
    ): List<ContentResponse>
    fun contentsListToMiniContentResponsesList(
        contentsList: List<Content>
    ): List<MiniContentResponse>
    fun contentsListToShortContentResponsesList(
        contentsList: List<Content>
    ): List<ShortContentResponse>
}
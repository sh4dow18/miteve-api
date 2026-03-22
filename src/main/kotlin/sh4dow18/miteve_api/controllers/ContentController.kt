package sh4dow18.miteve_api.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.content.ContentService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.contents}")
class ContentController(private val contentService: ContentService) {
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: String) = contentService.findById(id)
}
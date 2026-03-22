package sh4dow18.miteve_api.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.container.ContainerService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.containers}")
class ContainerController(private val containerService: ContainerService) {
    @GetMapping("type/{typeId}")
    @ResponseBody
    fun findByContentType(@PathVariable typeId: Long) = containerService.findByContentType(typeId)
}
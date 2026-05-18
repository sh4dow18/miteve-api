package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import sh4dow18.miteve_api.dtos.container.ContainerRequest
import sh4dow18.miteve_api.services.container.ContainerService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.containers}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ContainerController(private val containerService: ContainerService) {
    @GetMapping
    @ResponseBody
    fun findAll() = containerService.findAll()
    @GetMapping("type/{typeId}")
    @ResponseBody
    fun findByContentType(@PathVariable typeId: Long) = containerService.findByContentType(typeId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody containerRequest: ContainerRequest) = containerService.insert(containerRequest)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: Long, @RequestBody containerRequest: ContainerRequest) = containerService.update(id, containerRequest)
}
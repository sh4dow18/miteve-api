package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.privilege.PrivilegeRequest
import sh4dow18.miteve_api.dtos.privilege.UpdatePrivilegeRequest
import sh4dow18.miteve_api.services.privilege.PrivilegeService

@RestController
@RequestMapping("\${endpoint.privileges}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class PrivilegeController(private val privilegeService: PrivilegeService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAll() = privilegeService.findAll()
    @GetMapping("{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findById(@PathVariable id: Long) = privilegeService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody request: PrivilegeRequest) = privilegeService.insert(request)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: Long, @RequestBody request: UpdatePrivilegeRequest) = privilegeService.update(id, request)
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = privilegeService.delete(id)
}

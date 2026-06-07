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
import sh4dow18.miteve_api.dtos.user.UpdateUserRequest
import sh4dow18.miteve_api.dtos.user.UserRequest
import sh4dow18.miteve_api.services.user.UserService

// Container Rest Controller
@RestController
@RequestMapping("\${endpoint.users}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class UserController(private val userService: UserService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAll() = userService.findAll()
    @GetMapping("{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findById(@PathVariable id: Long) = userService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun register(@RequestBody userRequest: UserRequest) = userService.register(userRequest)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: Long, @RequestBody request: UpdateUserRequest) = userService.update(id, request)
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = userService.delete(id)
}
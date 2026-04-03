package sh4dow18.miteve_api.controllers

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${endpoint.utils}")
@CrossOrigin(origins = ["http://localhost:3000"])
class UtilsController {
    @RequestMapping("health", method = [RequestMethod.HEAD])
    fun health() {}
}
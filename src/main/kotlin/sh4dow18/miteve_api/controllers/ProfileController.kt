package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.services.profile.ProfileService

// Profile Rest Controller
@RestController
@RequestMapping("\${endpoint.profiles}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ProfileController(private val profileService: ProfileService) {
    @GetMapping("user/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAllByUserId(@PathVariable userId: Long) = profileService.findAllByUserId(userId)
    @GetMapping("{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findById(@PathVariable id: Long) = profileService.findById(id)
    @GetMapping("user/{userId}/main", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findMainProfileByUserId(@PathVariable userId: Long) = profileService.findMainProfileByUserId(userId)
    @GetMapping("{profileId}/continue-watching", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findContinueWatchingListByProfileId(@PathVariable profileId: Long) = profileService.findContinueWatchingListByProfileId(profileId)
}


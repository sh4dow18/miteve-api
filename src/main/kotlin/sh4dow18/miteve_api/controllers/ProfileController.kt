package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PutMapping
import sh4dow18.miteve_api.dtos.profile.ProfileRequest
import sh4dow18.miteve_api.dtos.profile.UpdateProfileRequest
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
    @PostMapping("user/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@PathVariable userId: Long, @RequestBody profileRequest: ProfileRequest) = profileService.insert(userId, profileRequest)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: Long, @RequestBody request: UpdateProfileRequest) = profileService.update(id, request)
    @GetMapping("{profileId}/favorites", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findFavoritesByProfileId(@PathVariable profileId: Long) = profileService.findFavoritesByProfileId(profileId)
    @PostMapping("{profileId}/favorites/{contentId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun addFavorite(@PathVariable profileId: Long, @PathVariable contentId: String) = profileService.addFavorite(profileId, contentId)
    @DeleteMapping("{profileId}/favorites/{contentId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun removeFavorite(@PathVariable profileId: Long, @PathVariable contentId: String) = profileService.removeFavorite(profileId, contentId)
}


package sh4dow18.miteve_api.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.comment.CommentRequest
import sh4dow18.miteve_api.dtos.comment.UpdateCommentRequest
import sh4dow18.miteve_api.services.comment.CommentService

// Comment Rest Controller
@RestController
@RequestMapping("\${endpoint.comments}")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class CommentController(private val commentService: CommentService) {
    @GetMapping("content/{contentId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findAllByContentId(@PathVariable contentId: String) = commentService.findAllByContentId(contentId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun insert(@RequestBody commentRequest: CommentRequest) = commentService.insert(commentRequest)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun update(@PathVariable id: Long, @RequestBody updateCommentRequest: UpdateCommentRequest) = commentService.update(id, updateCommentRequest)
}

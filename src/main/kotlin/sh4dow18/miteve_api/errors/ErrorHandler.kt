package sh4dow18.miteve_api.errors

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.lang.Exception;

// Error Handlers Main Class
@ControllerAdvice
class ErrorHandler: ResponseEntityExceptionHandler() {
    // Error handler when some element already exists and there cannot be a duplicate of it
    @ExceptionHandler(AlreadyExists::class)
    fun alreadyExists(exception: Exception) = getError(exception, HttpStatus.CONFLICT)
    // Error handler when some elements don't exist and needs to exist
    @ExceptionHandler(BadRequest::class)
    fun badRequest(exception: Exception) = getError(exception, HttpStatus.BAD_REQUEST)
    // Error handler when some elements don't exist and needs to exist
    @ExceptionHandler(NoExists::class)
    fun noExists(exception: Exception) = getError(exception, HttpStatus.NOT_FOUND)
    // Default error handler for each exception that does not have a specific error handler
    @ExceptionHandler(Exception::class, RuntimeException::class)
    fun default(exception: java.lang.Exception, ) = getError(exception, HttpStatus.INTERNAL_SERVER_ERROR)
    // Get Error Function
    fun getError(exception: Exception, status: HttpStatus): ResponseEntity<Any> {
        val apiError = ApiError(
            status = status,
            message = exception.message
        )
        logger.debug("Exception Found: {}", exception)
        return ResponseEntity(apiError, apiError.status)
    }
}
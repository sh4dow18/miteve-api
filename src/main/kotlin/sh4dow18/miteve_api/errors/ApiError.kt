package sh4dow18.miteve_api.errors

import org.springframework.http.HttpStatus

// "API Error" data class for use in the "Response Entity" constructor
data class ApiError(
    var status: HttpStatus,
    var message: String?
)

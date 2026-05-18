package sh4dow18.miteve_api.errors

// "Bad Request" class based on "Runtime Exception" class for use in error handlers with a template message
class BadRequest(message: String) : RuntimeException(message)
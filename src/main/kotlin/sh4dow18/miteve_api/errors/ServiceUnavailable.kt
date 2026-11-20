package sh4dow18.miteve_api.errors

// "Service Unavailable" class based on "Runtime Exception" class for use in error handlers with a template message
// Used when an upstream provider (e.g. lamovie.org) is down or has paused downloads,
// so callers can distinguish it from "content not found".
class ServiceUnavailable(message: String) : RuntimeException(message)

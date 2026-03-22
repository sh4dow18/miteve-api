package sh4dow18.miteve_api.errors

// "Already exists" class based on "Runtime Exception" class for use in error handlers with a template message
class AlreadyExists(element: String, existsAs: String) :
    RuntimeException("The element $element already exists as $existsAs")
package sh4dow18.miteve_api.errors

// "No Exists" class based on "Runtime Exception" class for use in error handlers with a template message
class NoExists(element: String, notExistsAs: String) :
    RuntimeException("The element with the id $element does not exists as $notExistsAs")
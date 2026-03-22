package sh4dow18.miteve_api.services.content

import sh4dow18.miteve_api.dtos.content.ContentResponse

// Content Service Interface where the functions to be used in
// Spring Abstract Content Service are declared
interface ContentService {
    fun findById(id: String): ContentResponse
}
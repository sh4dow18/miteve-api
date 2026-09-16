package sh4dow18.miteve_api.services.upload

import sh4dow18.miteve_api.dtos.upload.UploadResponse

interface AbstractUploadService {
    fun uploadToServer(filename: String): UploadResponse
    fun createFolder(path: String): Map<String, Any>
    fun verifyDash(slug: String, type: String, tmdbId: Long?): Map<String, Any>
    fun verifyPath(path: String): Map<String, Any>
}

package sh4dow18.miteve_api.dtos.upload

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String,
    val localPath: String,
    val remotePath: String,
    val commandOutput: String
)

package sh4dow18.miteve_api.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sh4dow18.miteve_api.dtos.ffmpeg.FfmpegRequest
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.services.ffmpeg.AbstractFfmpegService
import java.io.File
import java.nio.file.Paths

@RestController
@RequestMapping("\${endpoint.archive}", "/archive")
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ArchiveController(
    private val ffmpegService: AbstractFfmpegService,
    @Value("\${ffmpeg.work-dir:./videos}") private val workDir: String
) {

    @PostMapping("/unrar", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun unrar(@RequestBody req: FfmpegRequest) = ffmpegService.unrar(req.filename)

    @GetMapping("/exists", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun exists(@RequestParam filename: String): Map<String, Any> {
        val sanitized = sanitizePath(filename, listOf(".rar", ".mp4", ".mkv", ".mpd", ".vtt", ".m4s"))
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val file = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!file.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        return mapOf(
            "exists" to file.exists(),
            "filename" to sanitized,
            "path" to file.absolutePath,
            "isFile" to file.isFile,
            "size" to if (file.exists()) file.length() else 0
        )
    }

    @GetMapping("/rar/exists", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rarExists(@RequestParam filename: String): Map<String, Any> {
        val sanitized = sanitizePath(filename, listOf(".rar"))
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val file = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!file.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        return mapOf(
            "exists" to file.exists(),
            "filename" to sanitized,
            "path" to file.absolutePath,
            "isFile" to file.isFile,
            "size" to if (file.exists()) file.length() else 0
        )
    }

    @GetMapping("/mp4/exists", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun mp4Exists(@RequestParam filename: String): Map<String, Any> {
        val sanitized = sanitizePath(filename, listOf(".mp4"))
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val file = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!file.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        return mapOf(
            "exists" to file.exists(),
            "filename" to sanitized,
            "path" to file.absolutePath,
            "isFile" to file.isFile,
            "size" to if (file.exists()) file.length() else 0
        )
    }

    @GetMapping("/mkv/exists", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun mkvExists(@RequestParam filename: String): Map<String, Any> {
        val sanitized = sanitizePath(filename, listOf(".mkv"))
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val file = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!file.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        return mapOf(
            "exists" to file.exists(),
            "filename" to sanitized,
            "path" to file.absolutePath,
            "isFile" to file.isFile,
            "size" to if (file.exists()) file.length() else 0
        )
    }

    @GetMapping("/dash/exists", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dashExists(@RequestParam filename: String): Map<String, Any> {
        val sanitized = sanitizePath(filename, listOf(".mpd"))
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val file = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!file.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        return mapOf(
            "exists" to file.exists(),
            "filename" to sanitized,
            "path" to file.absolutePath,
            "isFile" to file.isFile,
            "size" to if (file.exists()) file.length() else 0
        )
    }

    private fun sanitizeArchive(name: String): String = sanitizePath(name, listOf(".rar"))

    private fun sanitizeArchivePath(name: String): String = sanitizePath(name, listOf(".rar"))

    private fun sanitizePath(name: String, allowedExts: List<String>): String {
        val trimmed = name.trim().trimStart('/')
        if (trimmed.isEmpty()) throw BadRequest("filename vacío")
        if (trimmed.contains("\\") || trimmed.contains("..")) throw BadRequest("filename inválido: $name")
        val parts = trimmed.split("/")
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) throw BadRequest("filename inválido: $name")
        val regex = Regex("^[a-zA-Z0-9._-]+$")
        for (p in parts) {
            val lower = p.lowercase()
            val hasAllowed = allowedExts.any { lower.endsWith(it) }
            if (!p.matches(regex) && !hasAllowed) throw BadRequest("filename inválido: $name (segmento $p)")
            if (hasAllowed) {
                val ext = allowedExts.first { lower.endsWith(it) }
                val base = p.dropLast(ext.length)
                if (!base.matches(regex)) throw BadRequest("filename inválido: $name")
            }
        }
        val lowerTrimmed = trimmed.lowercase()
        if (allowedExts.none { lowerTrimmed.endsWith(it) }) throw BadRequest("El archivo debe ser ${allowedExts.joinToString(" o ")}")
        return trimmed
    }
}

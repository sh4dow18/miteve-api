package sh4dow18.miteve_api.services.upload

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.upload.UploadResponse
import sh4dow18.miteve_api.errors.BadRequest
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.repositories.ContentRepository
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class UploadService(
    @Value("\${ffmpeg.work-dir:./videos}") private val workDir: String,
    @Value("\${upload.ssh.host:192.168.0.254}") private val sshHost: String,
    @Value("\${upload.ssh.user:sh4dow-server}") private val sshUser: String,
    @Value("\${upload.ssh.remote-path:/media/sh4dow-server/Miteve/stream/}") private val remotePath: String,
    @Value("\${upload.ssh.port:22}") private val sshPort: String,
    @Value("\${upload.ssh.password:}") private val sshPassword: String,
    @Value("\${upload.ssh.timeout-minutes:30}") private val timeoutMinutes: Long,
    private val contentRepository: ContentRepository
) : AbstractUploadService {

    override fun uploadToServer(filename: String): UploadResponse {
        val sanitized = sanitizePath(filename)
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        val localFile = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!localFile.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        if (!localFile.exists()) throw BadRequest("No existe $sanitized en $dir")
        val isDir = localFile.isDirectory
        val remoteParent = if (sanitized.contains("/")) {
            val parentRel = sanitized.substringBeforeLast("/")
            (remotePath.trimEnd('/') + "/" + parentRel).replace("//", "/")
        } else remotePath.trimEnd('/')
        val remoteTarget = if (isDir) {
            val parentOfDir = if (sanitized.contains("/")) sanitized.substringBeforeLast("/") else ""
            if (parentOfDir.isEmpty()) remotePath.trimEnd('/') else (remotePath.trimEnd('/') + "/" + parentOfDir).replace("//", "/")
        } else remoteParent
        if (sanitized.contains("/")) {
            val mkdirCmd = "mkdir -p \"$remoteTarget\""
            val sshTarget = "$sshUser@$sshHost"
            val password = resolvePassword()
            when {
                password.isBlank() -> exec(arrayOf("ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, mkdirCmd))
                isCommandAvailable("sshpass") -> exec(arrayOf("sshpass", "-p", password, "ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, mkdirCmd))
                isCommandAvailable("expect") -> {
                    val expectScript = "spawn ssh -p $sshPort -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $sshTarget \"$mkdirCmd\"; expect { \"password:\" { send \"$password\\r\"; exp_continue } eof }"
                    exec(arrayOf("expect", "-c", expectScript))
                }
                else -> throw BadRequest("sshpass no instalado para mkdir remoto")
            }
        }
        val remote = "$sshUser@$sshHost:$remoteTarget/"
        val password = resolvePassword()
        val useSshpass = password.isNotBlank() && isCommandAvailable("sshpass")
        val useExpect = password.isNotBlank() && !useSshpass && isCommandAvailable("expect")
        val output = when {
            password.isBlank() -> exec(arrayOf("scp", "-r", "-P", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", localFile.absolutePath, remote))
            useSshpass -> exec(arrayOf("sshpass", "-p", password, "scp", "-r", "-P", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", localFile.absolutePath, remote))
            useExpect -> {
                val expectScript = "spawn scp -r -P $sshPort -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${localFile.absolutePath} $remote; expect { \"password:\" { send \"$password\\r\"; exp_continue } eof }"
                exec(arrayOf("expect", "-c", expectScript))
            }
            else -> throw BadRequest("sshpass no instalado y se requiere password. Instala con: sudo apt-get install -y sshpass (o configura autenticación por clave SSH sin password). Password detectado vía \${UPLOAD_SSH_PASSWORD/MITEVE_SERVER_PASSWORD}")
        }
        val finalRemotePath = (remoteTarget + "/" + localFile.name).replace("//", "/")
        return UploadResponse(
            success = true,
            message = "Subido correctamente a $finalRemotePath",
            filename = sanitized,
            localPath = localFile.absolutePath,
            remotePath = "$sshUser@$sshHost:$finalRemotePath",
            commandOutput = output
        )
    }

    override fun createFolder(path: String): Map<String, Any> {
        val sanitized = sanitizePath(path)
        val remoteFull = (remotePath.trimEnd('/') + "/" + sanitized).replace("//", "/")
        val password = resolvePassword()
        val mkdirCmd = "mkdir -p \"$remoteFull\""
        val sshTarget = "$sshUser@$sshHost"
        val output = when {
            password.isBlank() -> exec(arrayOf("ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, mkdirCmd))
            isCommandAvailable("sshpass") -> exec(arrayOf("sshpass", "-p", password, "ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, mkdirCmd))
            isCommandAvailable("expect") -> {
                val expectScript = "spawn ssh -p $sshPort -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $sshTarget \"$mkdirCmd\"; expect { \"password:\" { send \"$password\\r\"; exp_continue } eof }"
                exec(arrayOf("expect", "-c", expectScript))
            }
            else -> throw BadRequest("sshpass no instalado y se requiere password para mkdir remoto. Instala: sudo apt-get install -y sshpass")
        }
        return mapOf(
            "success" to true,
            "message" to "Carpeta creada en servidor",
            "path" to sanitized,
            "remotePath" to remoteFull,
            "remote" to "$sshUser@$sshHost:$remoteFull",
            "output" to output
        )
    }

    private fun resolvePassword(): String {
        if (sshPassword.isNotBlank()) return sshPassword
        val envCandidates = listOf("MITEVE_SERVER_PASSWORD", "UPLOAD_SSH_PASSWORD", "SSH_PASSWORD", "SERVER_PASSWORD")
        for (key in envCandidates) {
            val v = System.getenv(key)
            if (!v.isNullOrBlank()) return v
        }
        return ""
    }

    private fun isCommandAvailable(cmd: String): Boolean {
        return try {
            val pb = ProcessBuilder("which", cmd).redirectErrorStream(true)
            val proc = pb.start()
            proc.waitFor(5, TimeUnit.SECONDS)
            proc.exitValue() == 0
        } catch (_: Exception) { false }
    }

    private fun exec(command: Array<String>): String {
        return try {
            val pb = ProcessBuilder(*command).redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(timeoutMinutes, TimeUnit.MINUTES)
            if (!finished) {
                proc.destroyForcibly()
                throw BadRequest("Timeout ejecutando scp (${timeoutMinutes}min): ${command.joinToString(" ")}")
            }
            if (proc.exitValue() != 0) {
                throw BadRequest("Error scp (${proc.exitValue()}): ${command.joinToString(" ").replace(Regex("-p\\s+\\S+"), "-p ***")}\n$output")
            }
            "$ ${command.joinToString(" ").replace(Regex("-p\\s+\\S+"), "-p ***")}\n$output"
        } catch (e: java.io.IOException) {
            if (e.message?.contains("No such file") == true || e.message?.contains("error=2") == true) {
                throw BadRequest("Comando no encontrado: ${command[0]}. Instala sshpass: sudo apt-get update && sudo apt-get install -y sshpass (o usa clave SSH sin password). Detalle: ${e.message}")
            }
            throw BadRequest("Error ejecutando ${command[0]}: ${e.message}")
        }
    }

    override fun verifyPath(path: String): Map<String, Any> {
        val sanitized = sanitizePath(path)
        val remoteFull = (remotePath.trimEnd('/') + "/" + sanitized).replace("//", "/")
        val exists = checkRemoteExists(remoteFull)
        return mapOf(
            "path" to sanitized,
            "remotePath" to remoteFull,
            "exists" to exists
        )
    }

    override fun verifyDash(slug: String, type: String, tmdbId: Long?): Map<String, Any> {
        val cleanSlug = sanitizePath(slug)
        val normalizedType = type.lowercase()
        if (normalizedType == "movie" || normalizedType == "pelicula" || normalizedType == "film") {
            val remoteFile = remotePath.trimEnd('/') + "/" + cleanSlug + "/manifest.mpd"
            val exists = checkRemoteExists(remoteFile)
            val dirExists = checkRemoteExists(remotePath.trimEnd('/') + "/" + cleanSlug, isDir = true)
            return mapOf(
                "slug" to cleanSlug,
                "type" to "movie",
                "remoteDir" to remotePath.trimEnd('/') + "/" + cleanSlug,
                "manifestPath" to remoteFile,
                "dirExists" to dirExists,
                "manifestExists" to exists,
                "verified" to exists
            )
        } else {
            val remoteBase = remotePath.trimEnd('/') + "/" + cleanSlug
            if (!checkRemoteExists(remoteBase, isDir = true)) {
                return mapOf(
                    "slug" to cleanSlug,
                    "type" to "tv",
                    "verified" to false,
                    "message" to "Serie no existe en servidor remoto",
                    "remoteBase" to remoteBase,
                    "seasons" to emptyList<Any>()
                )
            }
            val seasons = listRemoteDirs(remoteBase).filter { it.matches(Regex("^season-\\d+$")) }.sortedBy { it.substringAfter("season-").toIntOrNull() ?: 0 }
            if (seasons.isEmpty()) {
                return mapOf(
                    "slug" to cleanSlug,
                    "type" to "tv",
                    "verified" to false,
                    "message" to "No hay carpetas season en remoto",
                    "remoteBase" to remoteBase,
                    "seasons" to emptyList<Any>()
                )
            }
            val seasonResults = mutableListOf<Map<String, Any>>()
            var allExist = true
            var totalEpisodes = 0
            var missingManifests = 0
            for (seasonDir in seasons) {
                val seasonNum = seasonDir.substringAfter("season-").toIntOrNull() ?: 0
                val seasonPath = "$remoteBase/$seasonDir"
                val episodes = listRemoteDirs(seasonPath).filter { it.matches(Regex("^episode-\\d+$")) }.sortedBy { it.substringAfter("episode-").toIntOrNull() ?: 0 }
                val episodeResults = mutableListOf<Map<String, Any>>()
                for (epDir in episodes) {
                    val epNum = epDir.substringAfter("episode-").toIntOrNull() ?: 0
                    totalEpisodes++
                    val manifest = "$seasonPath/$epDir/manifest.mpd"
                    val exists = checkRemoteExists(manifest)
                    if (!exists) missingManifests++
                    if (!exists) allExist = false
                    episodeResults.add(mapOf(
                        "episode" to epNum,
                        "dir" to epDir,
                        "path" to "$cleanSlug/$seasonDir/$epDir/manifest.mpd",
                        "remotePath" to manifest,
                        "exists" to exists
                    ))
                }
                if (episodes.isEmpty()) allExist = false
                seasonResults.add(mapOf(
                    "season" to seasonNum,
                    "dir" to seasonDir,
                    "path" to "$cleanSlug/$seasonDir",
                    "remotePath" to seasonPath,
                    "episodeCount" to episodes.size,
                    "episodes" to episodeResults
                ))
            }
            return mapOf(
                "slug" to cleanSlug,
                "type" to "tv",
                "remoteBase" to remoteBase,
                "totalSeasons" to seasons.size,
                "totalEpisodes" to totalEpisodes,
                "missingManifests" to missingManifests,
                "allExist" to allExist,
                "verified" to allExist,
                "seasons" to seasonResults
            )
        }
    }

    private fun listRemoteDirs(remoteDir: String): List<String> {
        val cmdStr = "ls -1 \"$remoteDir\" 2>/dev/null || echo EMPTY"
        val sshTarget = "$sshUser@$sshHost"
        val password = resolvePassword()
        val output = when {
            password.isBlank() -> exec(arrayOf("ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, cmdStr))
            isCommandAvailable("sshpass") -> exec(arrayOf("sshpass", "-p", password, "ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, cmdStr))
            isCommandAvailable("expect") -> {
                val expectScript = "spawn ssh -p $sshPort -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $sshTarget \"$cmdStr\"; expect { \"password:\" { send \"$password\\r\"; exp_continue } eof }"
                exec(arrayOf("expect", "-c", expectScript))
            }
            else -> throw BadRequest("sshpass no instalado para listar remoto")
        }
        val lines = output.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("$") && it != "EMPTY" && !it.contains("StrictHostKeyChecking") && !it.contains("UserKnownHostsFile") }
        return lines.filter { it.matches(Regex("^[a-zA-Z0-9._-]+$")) }
    }

    private fun checkRemoteExists(remoteFullPath: String, isDir: Boolean = false): Boolean {
        val testFlag = if (isDir) "-d" else "-f"
        val cmdStr = "test $testFlag \"$remoteFullPath\" && echo EXISTS || echo MISSING"
        val sshTarget = "$sshUser@$sshHost"
        val password = resolvePassword()
        val output = when {
            password.isBlank() -> exec(arrayOf("ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, cmdStr))
            isCommandAvailable("sshpass") -> exec(arrayOf("sshpass", "-p", password, "ssh", "-p", sshPort, "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", sshTarget, cmdStr))
            isCommandAvailable("expect") -> {
                val expectScript = "spawn ssh -p $sshPort -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $sshTarget \"$cmdStr\"; expect { \"password:\" { send \"$password\\r\"; exp_continue } eof }"
                exec(arrayOf("expect", "-c", expectScript))
            }
            else -> throw BadRequest("sshpass no instalado para verificar remoto")
        }
        return output.contains("EXISTS")
    }

    private fun sanitizePath(name: String): String {
        val trimmed = name.trim().trimStart('/')
        if (trimmed.isEmpty()) throw BadRequest("filename vacío")
        if (trimmed.contains("\\") || trimmed.contains("..")) throw BadRequest("filename inválido: $name")
        val parts = trimmed.split("/")
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) throw BadRequest("filename inválido: $name")
        val regex = Regex("^[a-zA-Z0-9._-]+$")
        for (p in parts) {
            if (!p.matches(regex)) throw BadRequest("filename inválido: $name (segmento $p)")
        }
        return trimmed
    }
}

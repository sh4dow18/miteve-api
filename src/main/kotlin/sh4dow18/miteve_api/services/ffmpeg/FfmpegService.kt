package sh4dow18.miteve_api.services.ffmpeg

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sh4dow18.miteve_api.dtos.ffmpeg.FfmpegResponse
import sh4dow18.miteve_api.errors.BadRequest
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class FfmpegService(
    @Value("\${ffmpeg.work-dir:./videos}") private val workDir: String,
    @Value("\${ffmpeg.timeout-minutes:60}") private val timeoutMinutes: Long
) : AbstractFfmpegService {

    override fun transformTo1080p(filename: String, audioTrack: Int?, subtitleTrack: Int?): FfmpegResponse {
        val base = sanitize(filename)
        val dir = resolveWorkDir()
        val input = resolveInputFile(dir, base)
        val inputRel = input.relativeTo(dir).path
        val aIdx = audioTrack ?: 0
        val sIdx = subtitleTrack ?: 0
        val cmd = buildTransformAllFirst(base, inputRel, aIdx, sIdx)
        val log = exec(dir, cmd)
        return FfmpegResponse(true, "transform-to-1080p completado (a:$aIdx s:$sIdx)", base, listOf(log), listOf("$base-av1.mkv"))
    }

    override fun transformTo360p(filename: String): FfmpegResponse {
        val base = sanitize(filename)
        val dir = resolveWorkDir()
        val av1 = File(dir, "$base-av1.mkv")
        if (!av1.exists()) throw BadRequest("No existe $base-av1.mkv en $dir (requiere 1080p primero)")
        val cmd = buildTransformAllSecond(base)
        val log = exec(dir, cmd)
        return FfmpegResponse(true, "transform-to-360p completado", base, listOf(log), listOf("$base-low.mkv"))
    }

    override fun transformMkvsToHlsDash(filename: String): FfmpegResponse {
        val base = sanitize(filename)
        val dir = resolveWorkDir()
        val outputs = mutableListOf<String>()
        val logs = mutableListOf<String>()
        val av1 = File(dir, "$base-av1.mkv")
        val low = File(dir, "$base-low.mkv")
        if (!av1.exists()) throw BadRequest("No existe $base-av1.mkv en $dir")
        if (!low.exists()) throw BadRequest("No existe $base-low.mkv en $dir")
        val targetDir = File(dir, base)
        if (!targetDir.exists()) targetDir.mkdirs()
        val cmdSubs = arrayOf("ffmpeg", "-y", "-i", "$base-av1.mkv", "-map", "0:s:0?", "$base-subs.vtt")
        try {
            logs += exec(dir, cmdSubs)
            outputs += "$base-subs.vtt"
        } catch (e: Exception) {
            logs += "sin subtítulos, skip subs.vtt: ${e.message?.take(300)}"
        }
        val cmdDash = arrayOf(
            "ffmpeg", "-y",
            "-i", "$base-av1.mkv",
            "-i", "$base-low.mkv",
            "-map", "0:v", "-map", "0:a", "-map", "1:v",
            "-c:v", "copy", "-c:a", "copy",
            "-f", "dash",
            "-seg_duration", "6",
            "-use_template", "1",
            "-use_timeline", "1",
            "-streaming", "1",
            "-hls_playlist", "1",
            "-adaptation_sets", "id=0,streams=0,2 id=1,streams=1",
            "$base/manifest.mpd"
        )
        logs += exec(dir, cmdDash)
        outputs += "$base/manifest.mpd"
        val subsVtt = File(dir, "$base-subs.vtt")
        val destSubs = File(targetDir, "subs.vtt")
        var hasSubs = false
        if (subsVtt.exists()) {
            subsVtt.renameTo(destSubs)
            outputs += "$base/subs.vtt"
            hasSubs = destSubs.exists()
        }
        val manifest = File(targetDir, "manifest.mpd")
        if (manifest.exists() && hasSubs) {
            val content = manifest.readText()
            val patched = content.replace(
                "</Period>",
                "  <AdaptationSet mimeType=\"text/vtt\" lang=\"es\">\n      <Representation id=\"subtitles_es\" bandwidth=\"256\">\n        <BaseURL>subs.vtt</BaseURL>\n      </Representation>\n    </AdaptationSet>\n</Period>"
            )
            manifest.writeText(patched)
            logs += "patched manifest.mpd with subtitles AdaptationSet"
        } else if (manifest.exists()) {
            logs += "manifest sin subs, no patch"
        }
        return FfmpegResponse(true, "transform-mkvs-to-hls-dash completado", base, logs, outputs)
    }

    override fun unrar(filename: String): FfmpegResponse {
        val sanitized = sanitizeArchivePath(filename)
        val dir = resolveWorkDir()
        val archive = Paths.get(dir.absolutePath, sanitized).normalize().toFile()
        if (!archive.absolutePath.startsWith(dir.absolutePath)) throw BadRequest("filename inválido: $filename")
        if (!archive.exists()) throw BadRequest("No existe $sanitized en $dir")
        if (!sanitized.lowercase().endsWith(".rar")) throw BadRequest("El archivo debe ser .rar")
        val extractionDir = archive.parentFile ?: dir
        if (!extractionDir.exists()) extractionDir.mkdirs()
        val before = extractionDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val rarBase = archive.nameWithoutExtension
        val cmd = arrayOf("unrar", "x", "-pcc", "-o+", archive.absolutePath)
        val log = exec(extractionDir, cmd)
        val after = extractionDir.listFiles()?.toList() ?: emptyList()
        val newEntries = after.filter { it.name !in before }
        val logs = mutableListOf(log)
        val outputs = mutableListOf<String>()
        if (newEntries.size == 1 && newEntries[0].isFile) {
            val extractedFile = newEntries[0]
            val targetName = "$rarBase.mp4"
            val target = File(extractionDir, targetName)
            if (extractedFile.absolutePath != target.absolutePath) {
                if (target.exists()) target.delete()
                extractedFile.renameTo(target)
                logs += "renombrado ${extractedFile.name} -> $targetName (flujo archivo único)"
            }
            outputs += target.relativeTo(dir).path
        } else if (newEntries.size == 1 && newEntries[0].isDirectory) {
            val folder = newEntries[0]
            val expected = File(folder, folder.name + ".mp4")
            var source: File? = null
            if (expected.exists() && expected.isFile) source = expected
            else {
                val candidates = folder.listFiles()?.filter { it.isFile } ?: emptyList()
                source = candidates.find { it.nameWithoutExtension == folder.name }
                    ?: candidates.maxByOrNull { it.length() }
            }
            if (source != null && source.exists()) {
                val targetName = "$rarBase.mp4"
                val target = File(extractionDir, targetName)
                if (target.exists()) target.delete()
                val moved = source.renameTo(target)
                if (!moved) {
                    source.copyTo(target, overwrite = true)
                    source.delete()
                }
                logs += "movido ${folder.name}/${source.name} -> $targetName (flujo carpeta)"
                outputs += target.relativeTo(dir).path
                try {
                    folder.deleteRecursively()
                    logs += "carpeta basura eliminada: ${folder.name}"
                } catch (e: Exception) {
                    logs += "error borrando carpeta basura ${folder.name}: ${e.message}"
                }
            } else {
                logs += "carpeta ${folder.name} sin archivo coincidente, se deja intacta"
                outputs += folder.relativeTo(dir).path
            }
        } else {
            for (f in newEntries) outputs += f.relativeTo(dir).path
            if (newEntries.isEmpty()) logs += "sin nuevos archivos detectados (posible sobrescritura)"
        }
        return FfmpegResponse(true, "unrar x completado con password cc", sanitized, logs, outputs)
    }

    private fun sanitizeArchive(name: String): String = sanitizeArchivePath(name)

    private fun sanitizeArchivePath(name: String): String {
        val trimmed = name.trim().trimStart('/')
        if (trimmed.isEmpty()) throw BadRequest("filename vacío")
        if (trimmed.contains("\\") || trimmed.contains("..")) throw BadRequest("filename inválido: $name")
        val parts = trimmed.split("/")
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) throw BadRequest("filename inválido: $name")
        val regex = Regex("^[a-zA-Z0-9._-]+$")
        for (p in parts) {
            val isLast = p == parts.last()
            val check = if (isLast) p else p
            if (!check.matches(regex) && !(isLast && check.lowercase().endsWith(".rar") && check.dropLast(4).matches(regex))) {
                if (!p.matches(regex) && !p.lowercase().endsWith(".rar")) throw BadRequest("filename inválido: $name (segmento $p)")
            }
        }
        if (!trimmed.lowercase().endsWith(".rar")) throw BadRequest("El archivo debe ser .rar")
        return trimmed
    }

    private fun buildTransformAllFirst(base: String, inputName: String = "$base.mkv", audioTrack: Int = 0, subtitleTrack: Int = 0): Array<String> {
        return arrayOf(
            "ffmpeg", "-y",
            "-fflags", "+genpts",
            "-i", inputName,
            "-map", "0:v:0",
            "-map", "0:a:$audioTrack?",
            "-map", "0:s:$subtitleTrack?",
            "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1",
            "-c:v", "libsvtav1",
            "-crf", "33",
            "-preset", "6",
            "-pix_fmt", "yuv420p",
            "-c:a", "libopus",
            "-b:a", "112k",
            "-ac", "2",
            "-af", "aresample=async=1:first_pts=0",
            "$base-av1.mkv"
        )
    }

    private fun resolveInputFile(dir: File, base: String): File {
        val candidates = listOf("$base.mkv", "$base.mp4", "$base.MKV", "$base.MP4", "$base.avi", "$base.mov")
        for (c in candidates) {
            val f = File(dir, c)
            if (f.exists()) return f
        }
        throw BadRequest("No existe $base.mkv ni $base.mp4 en $dir")
    }

    private fun buildTransformAllSecond(base: String): Array<String> {
        return arrayOf(
            "ffmpeg", "-y",
            "-i", "$base-av1.mkv",
            "-map", "0:v:0",
            "-vf", "scale=640:360:force_original_aspect_ratio=decrease,pad=640:360:(ow-iw)/2:(oh-ih)/2,setsar=1",
            "-c:v", "libsvtav1",
            "-preset", "6",
            "-crf", "34",
            "-maxrate", "400k",
            "-bufsize", "800k",
            "-pix_fmt", "yuv420p10le",
            "-svtav1-params", "lp=0:tile-columns=1:tile-rows=1",
            "$base-low.mkv"
        )
    }

    private fun exec(workDir: File, command: Array<String>): String {
        val pb = ProcessBuilder(*command)
            .directory(workDir)
            .redirectErrorStream(true)
        pb.environment()["FFREPORT"] = ""
        val proc = pb.start()
        val output = proc.inputStream.bufferedReader().readText()
        val finished = proc.waitFor(timeoutMinutes, TimeUnit.MINUTES)
        if (!finished) {
            proc.destroyForcibly()
            throw BadRequest("Timeout ejecutando: ${command.joinToString(" ")}")
        }
        if (proc.exitValue() != 0) {
            throw BadRequest("Error ffmpeg (${proc.exitValue()}): ${command.joinToString(" ")}\n$output")
        }
        return "$ ${command.joinToString(" ")}\n$output"
    }

    private fun sanitize(name: String): String {
        var trimmed = name.trim().trimStart('/')
        if (trimmed.isEmpty()) throw BadRequest("filename vacío")
        if (trimmed.contains("\\") || trimmed.contains("..")) throw BadRequest("filename inválido: $name")
        if (trimmed.lowercase().endsWith(".mp4")) trimmed = trimmed.dropLast(4)
        if (trimmed.lowercase().endsWith(".mkv")) trimmed = trimmed.dropLast(4)
        val parts = trimmed.split("/")
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) throw BadRequest("filename inválido: $name")
        val regex = Regex("^[a-zA-Z0-9._-]+$")
        for (p in parts) {
            if (!p.matches(regex)) throw BadRequest("filename inválido: $name (segmento $p)")
        }
        return trimmed
    }

    private fun resolveWorkDir(): File {
        val dir = Paths.get(workDir).toAbsolutePath().normalize().toFile()
        if (!dir.exists()) dir.mkdirs()
        if (!dir.isDirectory) throw BadRequest("workDir no es directorio: $workDir")
        return dir
    }
}

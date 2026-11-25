package helpers

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.roundToLong


fun probeFileLength(file: File): Long? {
    return try {
        val process = ProcessBuilder(
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "csv=p=0",
            file.absolutePath
        ).start()
        val seconds = process.inputStream.bufferedReader().readText().trim().toDouble()
        (seconds * 1000).roundToLong()
    } catch (e: Exception){
        null
    }
}

fun probeAudioCodec(file: File): String? {
    return try {
        val process = ProcessBuilder(
            "ffprobe", "-v", "error",
            "-select_streams", "a:0",
            "-show_entries", "stream=codec_name",
            "-of", "csv=p=0",
            file.absolutePath
        ).start()
        process.inputStream.bufferedReader().readText().trim().ifBlank { null }
    } catch (e: Exception) {
        null
    }
}

fun getMetadataOrFileName(file: File): String {
    return try {
        val process = ProcessBuilder(
            "ffprobe", "-v", "error",
            "-show_entries", "format_tags=title",
            "-of", "csv=p=0",
            file.absolutePath
        ).start()
        return process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        file.nameWithoutExtension
    }
}

private val pickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun pickFilesAsync(formats: Set<String> = setOf("mp3", "flac", "m4a", "m4b", "aac", "ogg", "opus", "wav", "wma", "txt"), desc: String = "Audio or metadata files", multiple: Boolean = true, onResult: (List<String>) -> Unit) {
    pickerScope.launch { 
        val files = FileKit.openFilePicker(
            type = FileKitType.File(extensions = formats),
            mode = if (multiple) FileKitMode.Multiple() else FileKitMode.Multiple(maxItems = 1),
            title = desc,
        )?.sortedWith(compareBy(naturalOrder) { it.nameWithoutExtension })?.map { it.path } ?: emptyList()
        onResult(files)
    }
}

fun pickDir(desc: String = "Select folder"): String? = runBlocking {
    FileKit.openDirectoryPicker(title = desc)?.path
}

private val chunkRegex = Regex("\\d+|\\D+")

val naturalOrder = Comparator<String> { a, b ->
    val aParts = chunkRegex.findAll(a.lowercase()).map { it.value }.toList()
    val bParts = chunkRegex.findAll(b.lowercase()).map { it.value }.toList()

    for (i in 0 until minOf(aParts.size, bParts.size)) {
        val x = aParts[i]
        val y = bParts[i]
        val bothNumeric = x[0] in '0'..'9' && y[0] in '0'..'9'

        val cmp = if (bothNumeric)
            x.toBigInteger().compareTo(y.toBigInteger())
        else
            x.compareTo(y)
        if (cmp != 0) return@Comparator cmp
    }

    aParts.size - bParts.size
}
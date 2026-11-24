package helpers

import androidx.compose.ui.text.toLowerCase
import java.awt.Frame
import java.io.File
import java.util.Locale
import java.util.Locale.getDefault
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
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

fun selectFile(formats: Set<String> = setOf("mp3", "flac", "m4a", "m4b", "aac", "ogg", "opus", "wav", "wma", "txt"), desc: String = "Audio or metadata files"): List<String> {
    val frames = Frame.getFrames()
    val parent = frames.firstOrNull { it.isShowing }

    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Select a file"
    fileChooser.isMultiSelectionEnabled = true
    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
    fileChooser.fileFilter = FileNameExtensionFilter(desc, *formats.toTypedArray())

    val result = fileChooser.showDialog(parent, "Select file")

    return if (result == JFileChooser.APPROVE_OPTION) fileChooser.selectedFiles.sortedWith(compareBy(naturalOrder) {it.nameWithoutExtension}).map { it.path } else listOf<String>()
}

private val chunkRegex = Regex("\\d+|\\D+")

val naturalOrder = Comparator<String> { a, b ->
    val aParts = chunkRegex.findAll(a.lowercase()).map { it.value }.toList()
    val bParts = chunkRegex.findAll(b.lowercase()).map { it.value }.toList()
    
    for (i in 0 until minOf(aParts.size, bParts.size)) {
        val x = aParts[i]
        val y = bParts[i]
        val cmp = if (x[0].isDigit() == y[i].isDigit())
            x.toBigInteger().compareTo(y.toBigInteger())
        else
            x.compareTo(y)
        if (cmp != 0) return@Comparator cmp
    }
    
    aParts.size - bParts.size
}
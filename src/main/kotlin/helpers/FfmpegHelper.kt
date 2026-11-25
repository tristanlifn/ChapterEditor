package helpers

import androidx.compose.runtime.MutableState
import runCommand
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val escapeChars = listOf('=', ';', '#', '\\', '\n')

fun escapeFfmetadata(s: String): String {
    var escaped = StringBuilder()
    for (i in s.indices) {
        if (escapeChars.contains(s[i])) {
            escaped.append("\\")
        }
        escaped.append(s[i])
    }
    
    return escaped.toString()
}

fun unescapeFfmetadata(s: String): String {
    val unescaped = StringBuilder()
    var i = 0
    while (i < s.length) {
        if (s[i] == '\\' && i + 1 < s.length) {
            unescaped.append(s[i + 1])  // keep the escaped char, drop the backslash
            i += 2                       // skip both
        } else {
            unescaped.append(s[i])
            i++
        }
    }
    return unescaped.toString()
}

fun exportMetadataToFile(file: String, tmpPath: Path? = null): String {
    val tmpdir = tmpPath ?: Files.createTempDirectory("ChapterEditor")
        
    val workingDir = File(file).absoluteFile.parentFile ?: File(".")
    val metadataFilePath = "$tmpdir/metadata.txt"
    listOf("ffmpeg", "-i", file, "-f", "ffmetadata", metadataFilePath).runCommand(workingDir)
    
    return metadataFilePath
}
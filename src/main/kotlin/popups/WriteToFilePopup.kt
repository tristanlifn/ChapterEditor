package popups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import buildMetadataContent
import models.Metadata
import java.io.File
import selectFile

class FfmpegProgress {
    var writing by mutableStateOf(false)
    var outTimeUs by mutableStateOf(0L)
    var speed by mutableStateOf("")
    var done by mutableStateOf(false)
    var error by mutableStateOf("")

    val elapsed: String
        get() {
            val totalSeconds = outTimeUs / 1_000_000
            return "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
        }
}

@Composable
fun writeMetadataToFile(metadata: MutableState<Metadata>, onDismiss: () -> Unit, onResetState: () -> Unit) {
    var inputFilePath by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf("") }
    var outputFileName by remember { mutableStateOf("") }
    var outputFilePath by remember { mutableStateOf("") }
    val progress = remember { FfmpegProgress() }

    val coverExtensions = setOf("png", "jpg", "jpeg")

    LaunchedEffect(progress.done, progress.error) {
        if (progress.done && progress.error.isBlank()) onResetState()
    }

    PopupBox(
        popupWidth = 560F,
        popupHeight = 380F,
        showPopup = true,
        onClickOutside = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (progress.error.isNotBlank()) {
                    Text(
                        "Error writing metadata",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        progress.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                } else if (progress.writing) {
                    Text("Writing metadata...", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text("Encoded: ${progress.elapsed}", style = MaterialTheme.typography.bodyLarge)
                    Text("Speed: ${progress.speed}", style = MaterialTheme.typography.bodySmall)
                } else if (progress.done) {
                    Text("Completed", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Metadata written to ${outputFilePath.ifBlank { "unknown location" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                } else {
                    Text("Export metadata to audio file", style = MaterialTheme.typography.titleLarge)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputFilePath,
                            onValueChange = { inputFilePath = it },
                            label = { Text("Audio file") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { inputFilePath = selectFile() ?: inputFilePath }) {
                            Text("Select")
                        }
                    }

                    Row (
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = coverImage,
                            onValueChange = { coverImage = it },
                            label = { Text("Cover image (optional)") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { coverImage = selectFile(coverExtensions) ?: coverImage}) {
                            Text("Select")
                        }
                    }

                    OutlinedTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        label = { Text("Output file name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = inputFilePath.isNotBlank() && outputFileName.isNotBlank(),
                            onClick = {
                                // error messages and dont continue
                                if (!isFileNameValid(outputFileName)) {
                                    return@Button
                                }

                                val inputFile = File(inputFilePath).absoluteFile
                                val outputFile = File(inputFile.parentFile ?: File("."), "$outputFileName.m4b")
                                outputFilePath = outputFile.absolutePath
                                val tmpDir = kotlin.io.path.createTempDirectory("ChapterEditorExport")
                                val metadataFilePath = "$tmpDir/metadata.txt"
                                File(metadataFilePath).writeText(buildMetadataContent(metadata.value))

                                val hasCover = coverImage.isNotBlank()
                                val command = mutableListOf(
                                    "ffmpeg", "-i", inputFile.absolutePath,
                                    "-i", metadataFilePath
                                )
                                if (hasCover) {
                                    command.add("-i")
                                    command.add(coverImage)
                                }
                                command.add("-map_metadata")
                                command.add(if (hasCover) "2" else "1")
                                command.add("-codec:a")
                                command.add("aac")
                                command.add("-b:a")
                                command.add("64k")
                                command.add("-f")
                                command.add("mp4")
                                command.add("-progress")
                                command.add("pipe:1")
                                command.add("-nostats")
                                command.add("-loglevel")
                                command.add("error")
                                command.add(outputFile.absolutePath)

                                progress.writing = true
                                Thread {
                                    runFfmpegWithProgress(
                                        command = command,
                                        workingDir = inputFile.parentFile ?: File("."),
                                        onUpdate = { outTimeUs, speed, done, error ->
                                            if (outTimeUs > 0) progress.outTimeUs = outTimeUs
                                            if (speed.isNotBlank()) progress.speed = speed
                                            if (error.isNotBlank()) progress.error = error
                                            if (done) {
                                                progress.done = true
                                                progress.writing = false
                                            }
                                        }
                                    )
                                }.start()
                            }
                        ) {
                            Text("Write")
                        }
                    }
                }
            }
        }
    )
}

fun isFileNameValid(path: String): Boolean {
    if (path.isEmpty())
        return false

    val regex = Regex("^[A-Za-z0-9\\-_ ()]+$")
    return regex.matches(path)
}

fun runFfmpegWithProgress(
    command: List<String>,
    workingDir: File,
    onUpdate: (outTimeUs: Long, speed: String, done: Boolean, error: String) -> Unit
) {
    val process = ProcessBuilder(command)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val errorOutput = StringBuilder()
    val errorReader = Thread {
        process.errorStream.bufferedReader().forEachLine { line ->
            errorOutput.appendLine(line)
        }
    }.apply { start() }

    process.inputStream.bufferedReader().forEachLine { line ->
        when {
            line.startsWith("out_time_us=") || line.startsWith("out_time_ms=") -> {
                val us = line.substringAfter('=').toLongOrNull()
                if (us != null) onUpdate(us, "", false, "")
            }
            line.startsWith("speed=") -> onUpdate(0, line.substringAfter('=').trim(), false, "")
            line.startsWith("progress=") -> onUpdate(0, "", line.substringAfter('=').trim() == "end", "")
        }
    }

    process.waitFor()
    errorReader.join()

    if (process.exitValue() != 0) {
        onUpdate(0, "", false, errorOutput.toString().trim())
    } else {
        onUpdate(0, "", true, "")
    }
}

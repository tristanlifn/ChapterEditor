import models.Chapter
import models.Metadata
import helpers.probeFileLength
import popups.writeMetadataToFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import helpers.getMetadataOrFileName
import helpers.selectFile
import models.AudioSource
import java.awt.Frame
import java.io.File
import java.lang.String.format
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Models.Chapter Editor",
            state = rememberWindowState()
        ) {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppContent()
            }
        }
    } 
}

@Composable
fun AppContent() {
    val metadata = remember { mutableStateOf(
        Metadata(
            mutableStateListOf(), timeBase = "1/1000",
            date = "", title = "", artist = "", album = "", 
            albumArtist = "", comment = "", composer = "", sources = mutableStateListOf(), 
        )
    ) }
    
    var showExportPopup by remember { mutableStateOf(false) }

    val actions = listOf(
        AppAction("Import metadata", Icons.Filled.FileOpen) {
            getMetadataFromFile(metadata.value) { metadata.value = it }
        },
        AppAction("Export metadata to audio file", Icons.Filled.FileUpload) {
            showExportPopup = true
        },
        AppAction("Export metadata to txt file", Icons.Filled.FileDownload) {
            buildMetadataFile(metadata)
        },
    )
    
    fun addSource() {
        val filesPaths = selectFile(setOf("mp3", "flac", "m4a", "m4b", "aac", "ogg", "opus", "wav", "wma"))
        if (filesPaths.isEmpty()) return
        
        for (filePath in filesPaths) {
            val file = File(filePath)
            val durationMs = probeFileLength(file) ?: continue
    
            val nextId: Int = metadata.value.chapters.maxOfOrNull { it.id }?.plus(1) ?: 0
            val sourceId = metadata.value.sources.maxOfOrNull { it.id }?.plus(1) ?: 0
    
            metadata.value.sources.add(AudioSource(id = sourceId, path = file.path, durationMs = durationMs))
            metadata.value.chapters.add(Chapter(id = nextId, title = file.nameWithoutExtension, startTime = 0, endTime = convertMsToTimebase(durationMs, metadata.value.timeBase), sourceId = sourceId))
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            val useMenu = maxWidth < 1200.dp

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chapter Editor", style = MaterialTheme.typography.headlineMedium)
                    if (useMenu) {
                        ActionsMenu(actions)
                    } else {
                        Row (horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            actions.forEach { action ->
                                AppActionButton(action.label, action.icon, action.onClick)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FileMetadata(metadata, onChange = { metadata.value = it })
    
                    Divider(color = MaterialTheme.colorScheme.secondary, thickness = 2.dp)
    
                    if (metadata.value.chapters.isEmpty()) {
                        BlankSlate(onSourceAdd = { addSource() })
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(metadata.value.sources.toList(), key = { it.id }) { source ->
                                AudioSourceCard(
                                    source = source,
                                    chapter = metadata.value.chapters.filter { it.sourceId == source.id },
                                    timebase = metadata.value.timeBase,
                                    onChapterAdd = {
                                        val startTime = metadata.value.chapters.filter { it.sourceId == source.id }.maxOfOrNull { it.endTime } ?: 0
                                        val nextId: Int = metadata.value.chapters.maxOfOrNull { it.id }?.plus(1) ?: 0
    
                                        val chapter = Chapter(id = nextId, title = "Chapter ${nextId + 1}", startTime = startTime, endTime = 0, sourceId = source.id)
                                        metadata.value.chapters.add(chapter)
                                    },
                                    onChapterChange = { chapter ->
                                        val chapterIndex = metadata.value.chapters.indexOf(chapter)
                                        metadata.value.chapters[chapterIndex] = metadata.value.chapters[chapterIndex].copy(title = chapter.title, startTime = chapter.startTime, endTime = chapter.endTime)
                                    },
                                    onChapterDelete = { chapter ->
                                        metadata.value.chapters.remove(chapter)
                                    },
                                    onDelete = { 
                                        metadata.value.chapters.removeAll { it.sourceId == source.id }
                                        metadata.value.sources.remove(source)
                                    },
                                )
                            }
                            
                            item { 
                                Button(
                                    onClick = { addSource() } 
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add audio file")
                                    Text("Add audio file")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showExportPopup) {
            writeMetadataToFile(metadata = metadata, onDismiss = { showExportPopup = false }, onResetState = {
                metadata.value = Metadata(
                    mutableStateListOf(), timeBase = "1/1000",
                    date = "", title = "", artist = "", album = "", 
                    albumArtist = "", comment = "", composer = "", sources = mutableStateListOf(),
                )
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileMetadata(metadata: MutableState<Metadata>, onChange: (Metadata) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    FlowRow (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        FlowColumn {
            OutlinedTextField(
                value = metadata.value.title,
                onValueChange = { onChange(metadata.value.copy(title = it)) },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
            OutlinedTextField(
                value = metadata.value.date,
                onValueChange = { onChange(metadata.value.copy(date = it)) },
                label = { Text("Date") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
        }
        FlowColumn {
            OutlinedTextField(
                value = metadata.value.artist,
                onValueChange = { onChange(metadata.value.copy(artist = it)) },
                label = { Text("Author") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = metadata.value.timeBase,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time base") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .widthIn(min = 280.dp, max = 280.dp)
                        .padding(2.dp, 0.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("1/1000") },
                        onClick = {
                            onChange(metadata.value.copy(timeBase = "1/1000"))
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("1/100") },
                        onClick = {
                            onChange(metadata.value.copy(timeBase = "1/100"))
                            expanded = false
                        }
                    )
                }
            }
        }
        FlowColumn {
            OutlinedTextField(
                value = metadata.value.album,
                onValueChange = { onChange(metadata.value.copy(album = it)) },
                label = { Text("Series") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
            OutlinedTextField(
                value = metadata.value.albumArtist,
                onValueChange = { onChange(metadata.value.copy(albumArtist = it)) },
                label = { Text("Series artist") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
        }
        FlowColumn {
            OutlinedTextField(
                value = metadata.value.comment,
                onValueChange = { onChange(metadata.value.copy(comment = it)) },
                label = { Text("Comment") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
            OutlinedTextField(
                value = metadata.value.composer,
                onValueChange = { onChange(metadata.value.copy(composer = it)) },
                label = { Text("Narrator") },
                singleLine = true,
                modifier = Modifier.widthIn(min = 280.dp, max = 280.dp)
                    .padding(2.dp, 0.dp)
            )
        }
    }
}

@Composable
fun BlankSlate(onSourceAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No chapters yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Click \"Add audio file\" to add your first one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onSourceAdd,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add audio file")
                Spacer(Modifier.width(8.dp))
                Text("Add audio file")
            }
        }
    }
}

@Composable
fun AudioSourceCard(
    source: AudioSource,
    chapter: List<Chapter>,
    timebase: String,
    onChapterAdd: () -> Unit,
    onChapterChange: (Chapter) -> Unit,
    onChapterDelete: (Chapter) -> Unit,
    onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row (
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column (
                    horizontalAlignment = Alignment.Start,
                ) { 
                    Text(getMetadataOrFileName(File(source.path)))
                }
                Column (
                    horizontalAlignment = Alignment.End,
                ) { 
                    val length = source.durationMs.milliseconds
                    print(source.durationMs)
                    
                    Text(length.toComponents { hours, minutes, seconds, _ ->
                        format("%02d:%02d:%02d", hours, minutes, seconds) })

                }
            }
            chapter.forEach { chapter ->
                ChapterCard(
                    box = chapter, 
                    onChange = onChapterChange,
                    onDelete = { onChapterDelete(chapter) }
                )
            }
            Row {
                Column { 
                    IconButton(onClick = onChapterAdd) {
                        Icon(Icons.Default.Add, contentDescription = "Add chapter")
                    }
                }
                Column { 
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete source file and chapters")
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterCard(box: Chapter, onChange: (Chapter) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                OutlinedTextField(
                    value = box.title,
                    onValueChange = { onChange(box.copy(title = it)) },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = box.startTime.toString(),
                    onValueChange = { input ->
                        input.toLongOrNull()?.let { onChange(box.copy(startTime = it)) }
                    },
                    label = { Text("Start timestamp") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = box.endTime.toString(),
                    onValueChange = { input ->
                        input.toLongOrNull()?.let { onChange(box.copy(endTime = it)) }
                    },
                    label = { Text("End timestamp") },
                    singleLine = true
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${box.title}")
            }
        }
    }
}

fun getMetadataFromFile(metadata: Metadata, onChange: (Metadata) -> Unit) {
    val filePaths = selectFile()
    getMetadataFromFile(filePaths, metadata, onChange)
}

fun getMetadataFromFile(filePaths: List<String>, metadata: Metadata, onChange: (Metadata) -> Unit) {
    for (filePath in filePaths) {
        if (File(filePath).extension.equals("txt", ignoreCase = true)) {
            mapMetadataFile(File(filePath), metadata, onChange)
            continue
        }
        
        val file = File(filePath).absoluteFile.parentFile ?: File(".")
        val tmpDir = kotlin.io.path.createTempDirectory("ChapterEditor")
        val metadataFilePath = "$tmpDir/metadata.txt"
        
        listOf("ffmpeg", "-i", filePath, "-f", "ffmetadata", metadataFilePath).runCommand(file)
        mapMetadataFile(File(metadataFilePath), metadata, onChange)
    }
}

fun mapMetadataFile(file: File, metadata: Metadata, onChange: (Metadata) -> Unit) {
    val chapterKeys = setOf("TIMEBASE", "START", "END", "title")
    val chapters = mutableStateListOf<Chapter>()
    var chapterId = 0
    var result = metadata.copy(chapters = chapters)
    var inChapter = false

    file.forEachLine { line ->
        val trimmed = line.trim()
        val key = trimmed.substringBefore('=')

        when {
            trimmed == "[CHAPTER]" -> {
                chapterId += 1
                chapters.add(Chapter(id = chapterId, title = "", startTime = 0, endTime = 0))
                inChapter = true
            }
            inChapter && key in chapterKeys -> {
                when (key) {
                    "TIMEBASE" -> result = result.copy(timeBase = trimmed.substringAfter('='))
                    "START" -> {
                        val chapter = chapters.last()
                        chapters[chapters.lastIndex] =
                            chapter.copy(startTime = trimmed.substringAfter('=').toLongOrNull() ?: chapter.startTime)
                    }
                    "END" -> {
                        val chapter = chapters.last()
                        chapters[chapters.lastIndex] =
                            chapter.copy(endTime = trimmed.substringAfter('=').toLongOrNull() ?: chapter.endTime)
                    }
                    "title" -> {
                        val chapter = chapters.last()
                        chapters[chapters.lastIndex] = chapter.copy(title = trimmed.substringAfter('='))
                    }
                }
            }
            else -> {
                inChapter = false
                when (key) {
                    "date" -> result = result.copy(date = trimmed.substringAfter('='))
                    "title" -> result = result.copy(title = trimmed.substringAfter('='))
                    "artist" -> result = result.copy(artist = trimmed.substringAfter('='))
                    "album" -> result = result.copy(album = trimmed.substringAfter('='))
                    "album_artist" -> result = result.copy(albumArtist = trimmed.substringAfter('='))
                    "comment" -> result = result.copy(comment = trimmed.substringAfter('='))
                    "composer" -> result = result.copy(composer = trimmed.substringAfter('='))
                }
            }
        }
    }

    onChange(result)
}

fun buildMetadataContent(metadata: Metadata): String {
    var content =
                ";FFMETADATA1\n" +
                "encoder=Lavf63.1.100\n"

    if (metadata.date.isNotBlank())
        content += "date=${metadata.date}\n"
    if (metadata.title.isNotBlank())
        content += "title=${metadata.title}\n"
    if (metadata.artist.isNotBlank())
        content += "artist=${metadata.artist}\n"
    if (metadata.album.isNotBlank())
        content += "album=${metadata.album}\n"
    if (metadata.albumArtist.isNotBlank())
        content += "album_artist=${metadata.albumArtist}\n"
    if (metadata.comment.isNotBlank())
        content += "comment=${metadata.comment}\n"
    if (metadata.composer.isNotBlank())
        content += "composer=${metadata.composer}\n"

    for (chapter in resolveChapterTimestamp(metadata)){
        val chapterString =
                    "[CHAPTER]\n" +
                    "TIMEBASE=${metadata.timeBase}\n" +
                    "START=${chapter.startTime}\n" +
                    "END=${chapter.endTime}\n" +
                    "title=${chapter.title}\n"

        content += chapterString
    }

    return content
}

fun buildMetadataFile(metadata: MutableState<Metadata>) {
    val homeDir = System.getProperty("user.home") ?: System.getenv("HOME") ?: System.getProperty("user.dir")
    val outFile = File("$homeDir/Downloads/metadata.txt")
    outFile.createNewFile()
    outFile.writeText(buildMetadataContent(metadata.value))
}

fun List<String>.runCommand(workingDir: File) {
    ProcessBuilder(this)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

class AppAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun AppActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(icon, contentDescription = label)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun ActionsMenu(actions: List<AppAction>) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    }
                )
            }
        }
    }
}



fun convertMsToTimebase(length: Long, timebase: String): Long = when (timebase) {
    "1/1000" -> length
    "1/100" -> length / 10
    else -> error("Invalid time base: $timebase")
}

fun resolveChapterTimestamp(metadata: Metadata): List<Chapter> {
    val offsets = mutableMapOf<Int, Long>()
    var runningMs = 0L
    
    for (source in metadata.sources) {
        val timespanInTimebase = convertMsToTimebase(runningMs, metadata.timeBase)
        
        offsets[source.id] = timespanInTimebase
        runningMs += source.durationMs
    }
    
    return metadata.chapters.map { chapter ->
        val offset = chapter.sourceId?.let { offsets[it] } ?: 0
        chapter.copy(startTime = chapter.startTime + offset, endTime = chapter.endTime + offset)
    }.sortedBy { it.startTime }
}
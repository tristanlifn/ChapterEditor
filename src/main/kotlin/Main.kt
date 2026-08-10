import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Chapter Editor",
        state = rememberWindowState()
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val metadata = remember { mutableStateOf(Metadata(mutableStateListOf(), timeBase = "1/1000",
        date = "", title = "", artist = "", album = "", albumArtist = "", comment = "")) }
    var nextId by remember { mutableIntStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                Row (horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        buildMetadataFile(metadata)
                    }) {
                        Icon(Icons.Filled.FileDownload, "Build metadata file")
                        Spacer(Modifier.width(8.dp))
                        Text("Build metadata file")
                    }
                    Button(onClick = {
                        val startTime = metadata.value.chapters.lastOrNull()?.endTime ?: 0
                        val last: Int? = metadata.value.chapters.lastOrNull()?.id

                        nextId = if (last == null)
                            0
                        else
                            last + 1

                        val chapter = Chapter(id = nextId, title = "Chapter ${nextId + 1}", startTime = startTime, endTime = 0, timebase = metadata.value.timeBase)
                        metadata.value.chapters.add(chapter)
                        nextId += 1
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add chapter")
                        Spacer(Modifier.size(8.dp))
                        Text("Add chapter")
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Row (horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column { // TODO: add onValueChange method
                        OutlinedTextField(
                            value = metadata.value.date,
                            onValueChange = { },
                            label = { Text("Date") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = metadata.value.title,
                            onValueChange = { },
                            label = { Text("Title") },
                            singleLine = true
                        )
                    }
                    Column {
                        OutlinedTextField(
                            value = metadata.value.artist,
                            onValueChange = { },
                            label = { Text("Artist") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("") },
                            singleLine = true
                        )
                    }
                    Column {
                        OutlinedTextField(
                            value = metadata.value.album,
                            onValueChange = { },
                            label = { Text("Series") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = metadata.value.albumArtist,
                            onValueChange = { },
                            label = { Text("Series artist") },
                            singleLine = true
                        )
                    }
                    Column {
                        OutlinedTextField(
                            value = metadata.value.comment,
                            onValueChange = { },
                            label = { Text("Comment") },
                            singleLine = false
                        )
                    }
                }

                if (metadata.value.chapters.isEmpty()) {
                    BlankSlate()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(metadata.value.chapters.toList(), key = { it.id }) { box ->
                            ChapterCard(
                                box = box,
                                onChange = { updated ->
                                    val index = metadata.value.chapters.indexOfFirst { it.id == box.id }
                                    if (index >= 0) metadata.value.chapters[index] = updated
                                },
                                onDelete = {
                                    metadata.value.chapters.removeAll { it.id == box.id }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlankSlate() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No boxes yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Click \"Add box\" to create your first one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        input.toIntOrNull()?.let { onChange(box.copy(startTime = it)) }
                    },
                    label = { Text("Start timestamp") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = box.endTime.toString(),
                    onValueChange = { input ->
                        input.toIntOrNull()?.let { onChange(box.copy(endTime = it)) }
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

fun buildMetadataFile(metadata: MutableState<Metadata>){
    val homeDir = System.getProperty("user.home") ?: System.getenv("HOME") ?: System.getProperty("user.dir")
    val outFile = File("$homeDir/Downloads/metadata.txt")
    outFile.createNewFile()

    var content =
                ";FFMETADATA1\n" +
                "encoder=Lavf63.1.100\n"

    if (metadata.value.date.isNotBlank())
        content += "date=${metadata.value.date}\n"
    if (metadata.value.title.isNotBlank())
        content += "title=${metadata.value.title}\n"
    if (metadata.value.artist.isNotBlank())
        content += "artist=${metadata.value.artist}\n"
    if (metadata.value.album.isNotBlank())
        content += "album=${metadata.value.album}\n"
    if (metadata.value.albumArtist.isNotBlank())
        content += "album_artist=${metadata.value.albumArtist}\n"
    if(metadata.value.comment.isNotBlank())
        content += "comment=${metadata.value.comment}\n"

    for (chapter in metadata.value.chapters){
        val chapterString =
                    "[CHAPTER]\n" +
                    "TIMEBASE=${chapter.timebase}\n" +
                    "START=${chapter.startTime}\n" +
                    "END=${chapter.endTime}\n" +
                    "title=${chapter.title}\n"

        content += chapterString
    }

    outFile.writeText(content)
}

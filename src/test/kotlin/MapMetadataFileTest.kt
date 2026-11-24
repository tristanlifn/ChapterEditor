import models.Metadata
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class MapMetadataFileTest {

    @Test
    fun mapsGlobalMetadataAndChapters() {
        val file = metadataFile(
            """
            ;FFMETADATA1
            encoder=Lavf63.1.100
            date=2012
            title=The Hobbit (Unabridged)
            artist=J. R. R. Tolkien
            album_artist=J. R. R. Tolkien
            album=The Hobbit (Unabridged)
            comment=A long comment.
            [CHAPTER]
            TIMEBASE=1/22050
            START=0
            END=80362503
            title=CHAPTER I
            [CHAPTER]
            TIMEBASE=1/22050
            START=80362503
            END=128330757
            title=CHAPTER II
            [CHAPTER]
            TIMEBASE=1/22050
            START=128330757
            END=154206211
            title=CHAPTER III
            """.trimIndent() + "\n"
        )

        val result = parse(file)

        assertEquals("2012", result.date)
        assertEquals("The Hobbit (Unabridged)", result.title)
        assertEquals("J. R. R. Tolkien", result.artist)
        assertEquals("J. R. R. Tolkien", result.albumArtist)
        assertEquals("The Hobbit (Unabridged)", result.album)
        assertEquals("A long comment.", result.comment)
        assertEquals("1/22050", result.timeBase)
        assertEquals(3, result.chapters.size)
        assertEquals("CHAPTER I", result.chapters[0].title)
        assertEquals(0, result.chapters[0].startTime)
        assertEquals(80362503, result.chapters[0].endTime)
        assertEquals("CHAPTER II", result.chapters[1].title)
        assertEquals(80362503, result.chapters[1].startTime)
        assertEquals(128330757, result.chapters[1].endTime)
        assertEquals(1, result.chapters[0].id)
        assertEquals(3, result.chapters[2].id)
    }

    @Test
    fun mapsGlobalMetadataThatAppearsAfterChapters() {
        val file = metadataFile(
            """
            ;FFMETADATA1
            [CHAPTER]
            TIMEBASE=1/1000
            START=0
            END=1000
            title=Chapter One
            [CHAPTER]
            TIMEBASE=1/1000
            START=1000
            END=2000
            title=Chapter Two
            date=2024
            title=Whole Book Title
            artist=Some Artist
            """.trimIndent() + "\n"
        )

        val result = parse(file)

        assertEquals(2, result.chapters.size)
        assertEquals("Chapter Two", result.chapters.last().title)
        assertEquals("2024", result.date)
        assertEquals("Whole Book Title", result.title)
        assertEquals("Some Artist", result.artist)
        assertEquals("1/1000", result.timeBase)
    }

    @Test
    fun keepsExistingValuesForFieldsAbsentFromFile() {
        val file = metadataFile("title=Only Title\n")
        val existing = Metadata(
            mutableListOf(),
            "1/1000",
            "2020",
            "Old",
            "Old Artist",
            "Old Series Artist",
            "Old Album",
            "Old Comment",
            "Old Composer"
        )

        val results = mutableListOf<Metadata>()
        mapMetadataFile(file, existing) { results.add(it) }

        val result = results.single()
        assertEquals("Only Title", result.title)
        assertEquals("Old Artist", result.artist)
        assertEquals("Old Album", result.album)
        assertEquals("1/1000", result.timeBase)
    }

    private fun parse(file: File): Metadata {
        val results = mutableListOf<Metadata>()
        mapMetadataFile(file, emptyMetadata()) { results.add(it) }
        return results.single()
    }

    private fun emptyMetadata() = Metadata(mutableListOf(), "1/1000", "", "", "", "", "", "", "")

    private fun metadataFile(content: String): File =
        File.createTempFile("metadata", ".txt").apply { writeText(content) }
}

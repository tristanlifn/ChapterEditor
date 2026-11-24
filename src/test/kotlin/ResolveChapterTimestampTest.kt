import models.AudioSource
import models.Chapter
import models.Metadata
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveChapterTimestampTest {

    @Test
    fun absoluteChapterIsUnchangedWithoutSources() {
        val metadata = metadata(
            chapters = listOf(chapter(id = 1, start = 100, end = 200, sourceId = null)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(100, result.startTime)
        assertEquals(200, result.endTime)
    }

    @Test
    fun chapterInFirstSourceHasNoOffset() {
        val metadata = metadata(
            sources = listOf(source(id = 1, durationMs = 1000), source(id = 2, durationMs = 2000)),
            chapters = listOf(chapter(id = 1, start = 0, end = 500, sourceId = 1)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(0, result.startTime)
        assertEquals(500, result.endTime)
    }

    @Test
    fun chapterInSecondSourceIsShiftedByFirstSourceDuration() {
        val metadata = metadata(
            sources = listOf(source(id = 1, durationMs = 1000), source(id = 2, durationMs = 2000)),
            chapters = listOf(chapter(id = 1, start = 0, end = 500, sourceId = 2)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(1000, result.startTime)
        assertEquals(1500, result.endTime)
    }

    @Test
    fun chapterInThirdSourceIsShiftedBySumOfEarlierSources() {
        val metadata = metadata(
            sources = listOf(
                source(id = 1, durationMs = 1000),
                source(id = 2, durationMs = 2000),
                source(id = 3, durationMs = 4000),
            ),
            chapters = listOf(chapter(id = 1, start = 10, end = 20, sourceId = 3)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(3010, result.startTime)
        assertEquals(3020, result.endTime)
    }

    @Test
    fun offsetIsConvertedToCentisecondTimebase() {
        val metadata = metadata(
            timeBase = "1/100",
            sources = listOf(source(id = 1, durationMs = 1000), source(id = 2, durationMs = 2000)),
            chapters = listOf(chapter(id = 1, start = 0, end = 50, sourceId = 2)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(100, result.startTime)
        assertEquals(150, result.endTime)
    }

    @Test
    fun roundingErrorDoesNotAccumulateAcrossSources() {
        // 1005 ms + 1005 ms = 2010 ms -> 201 in 1/100.
        // Converting each duration first would give 100 + 100 = 200.
        val metadata = metadata(
            timeBase = "1/100",
            sources = listOf(
                source(id = 1, durationMs = 1005),
                source(id = 2, durationMs = 1005),
                source(id = 3, durationMs = 1005),
            ),
            chapters = listOf(chapter(id = 1, start = 0, end = 10, sourceId = 3)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(201, result.startTime)
    }

    @Test
    fun absoluteChapterIsNotShiftedWhenASourceHasIdZero() {
        val metadata = metadata(
            sources = listOf(source(id = 0, durationMs = 1000), source(id = 1, durationMs = 1000)),
            chapters = listOf(chapter(id = 1, start = 100, end = 200, sourceId = null)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(100, result.startTime)
        assertEquals(200, result.endTime)
    }

    @Test
    fun resultIsSortedByAbsoluteStartTime() {
        val metadata = metadata(
            sources = listOf(source(id = 1, durationMs = 1000), source(id = 2, durationMs = 2000)),
            chapters = listOf(
                chapter(id = 1, start = 0, end = 100, sourceId = 2),
                chapter(id = 2, start = 0, end = 100, sourceId = 1),
            ),
        )

        val result = resolveChapterTimestamp(metadata)

        assertEquals(listOf(2, 1), result.map { it.id })
    }

    @Test
    fun keepsIdTitleAndSourceId() {
        val metadata = metadata(
            sources = listOf(source(id = 1, durationMs = 1000), source(id = 2, durationMs = 2000)),
            chapters = listOf(Chapter(id = 7, title = "Intro", startTime = 0, endTime = 10, sourceId = 2)),
        )

        val result = resolveChapterTimestamp(metadata).single()

        assertEquals(7, result.id)
        assertEquals("Intro", result.title)
        assertEquals(2, result.sourceId)
    }

    private fun metadata(
        chapters: List<Chapter> = emptyList(),
        sources: List<AudioSource> = emptyList(),
        timeBase: String = "1/1000",
    ) = Metadata(
        chapters = chapters.toMutableList(),
        timeBase = timeBase,
        date = "",
        title = "",
        artist = "",
        albumArtist = "",
        album = "",
        comment = "",
        composer = "",
        sources = sources.toMutableList(),
    )

    private fun source(id: Int, durationMs: Long) =
        AudioSource(id = id, path = "/tmp/source$id.mp3", durationMs = durationMs)

    private fun chapter(id: Int, start: Long, end: Long, sourceId: Int?) =
        Chapter(id = id, title = "Chapter $id", startTime = start, endTime = end, sourceId = sourceId)
}

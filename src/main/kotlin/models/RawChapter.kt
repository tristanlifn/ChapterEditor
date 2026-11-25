package models

data class RawChapter(
    var timebase: String = "1/1000000000",
    var start: Long = 0L,
    var end: Long = 0L,
    var title: String = ""
)

package models

data class Metadata(
    val chapters: MutableList<Chapter>,
    val timeBase: String,
    val date: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val comment: String,
)
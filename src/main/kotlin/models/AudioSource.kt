package models

data class AudioSource(
    val id: Int,
    val path: String,
    val durationMs: Long,
)
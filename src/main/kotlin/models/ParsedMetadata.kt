package models

data class ParsedMetadata(
    val chapters: List<Chapter>,
    val tags: Map<String, String>
)
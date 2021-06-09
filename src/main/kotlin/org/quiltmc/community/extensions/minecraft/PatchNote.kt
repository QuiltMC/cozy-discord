package org.quiltmc.community.extensions.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class PatchNote(
    val body: String,
    val contentPath: String,
    val id: String,
    val image: PatchNoteImage,
    val title: String,
    val type: String,
    val version: String,
)

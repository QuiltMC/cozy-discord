package org.quiltmc.community.modes.quilt.extensions.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class PatchNoteImage(
    val title: String,
    val url: String,
)

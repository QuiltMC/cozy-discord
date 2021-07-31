package org.quiltmc.community.modes.quilt.extensions.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class PatchNoteEntries(
    val entries: List<PatchNote>,
    val version: Int,
)

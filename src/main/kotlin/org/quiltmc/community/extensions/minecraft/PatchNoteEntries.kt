package org.quiltmc.community.extensions.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class PatchNoteEntries(
    val entries: List<PatchNote>,
    val version: Int,
)

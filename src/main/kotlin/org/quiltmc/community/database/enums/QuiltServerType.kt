package org.quiltmc.community.database.enums

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class QuiltServerType(override val readableName: String) : ChoiceEnum {
    COMMUNITY("Community"),
    TOOLCHAIN("Toolchain")
}

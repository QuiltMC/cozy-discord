/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.plus
import org.quiltmc.community.cozy.modules.moderation.config.ModerationConfig
import org.quiltmc.community.cozy.modules.moderation.config.SimpleModerationConfig
import kotlin.time.DurationUnit

public operator fun DateTimePeriod.compareTo(other: DateTimePeriod): Int =
    this.toTotalSeconds().compareTo(other.toTotalSeconds())

public fun DateTimePeriod.toTotalSeconds(): Int {
    val now = Clock.System.now()
    return (now.plus(this, UTC) - now).toInt(DurationUnit.SECONDS)
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.moderation(config: ModerationConfig) {
    add { ModerationExtension(config) }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.moderation(body: SimpleModerationConfig.Builder.() -> Unit): Unit =
    moderation(SimpleModerationConfig(body))

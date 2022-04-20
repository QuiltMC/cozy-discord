/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.quiltmc.community.cozy.modules.welcome.blocks.*
import kotlin.time.Duration

public abstract class WelcomeChannelConfig {
    public val defaultSerializersModule: SerializersModule =
        SerializersModule {
            polymorphic(Block::class) {
                subclass(EmbedBlock::class)
                subclass(LinksBlock::class)
                subclass(RolesBlock::class)
                subclass(RulesBlock::class)
                subclass(TextBlock::class)
            }
        }

    /**
     * Get the configured staff command checks, used to ensure a staff-facing command can be run.
     */
    public abstract suspend fun getStaffCommandChecks(): List<Check<*>>

    public abstract suspend fun getRefreshDelay(): Duration?

    /**
     * Get the configured serializer module, which may be modified if other blocks have been set up.
     */
    public open suspend fun getSerializersModule(): SerializersModule = defaultSerializersModule
}

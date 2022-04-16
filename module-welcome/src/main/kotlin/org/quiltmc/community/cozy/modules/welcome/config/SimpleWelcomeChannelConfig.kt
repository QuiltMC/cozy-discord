/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import kotlin.time.Duration

public class SimpleWelcomeChannelConfig(private val builder: Builder) : WelcomeChannelConfig() {
    override suspend fun getStaffCommandChecks(): List<Check<*>> = builder.staffCommandChecks
    override suspend fun getRefreshDelay(): Duration? = builder.refreshDuration

    public class Builder {
        internal val staffCommandChecks: MutableList<Check<*>> = mutableListOf()

        public var refreshDuration: Duration? = null

        public fun staffCommandCheck(body: Check<*>) {
            staffCommandChecks.add(body)
        }
    }
}

public fun SimpleWelcomeChannelConfig(body: SimpleWelcomeChannelConfig.Builder.() -> Unit): SimpleWelcomeChannelConfig {
    val builder = SimpleWelcomeChannelConfig.Builder()

    body(builder)

    return SimpleWelcomeChannelConfig(builder)
}

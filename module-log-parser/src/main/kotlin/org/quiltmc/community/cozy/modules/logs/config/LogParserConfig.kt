/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.config

import com.kotlindiscord.kord.extensions.checks.types.Check

public abstract class LogParserConfig {
	/**
	 * Get the configured staff command checks, used to ensure a staff-facing command can be run.
	 */
	public abstract suspend fun getStaffCommandChecks(): List<Check<*>>

	/**
	 * Get the configured user command checks, used to ensure a user-facing command can be run.
	 */
	public abstract suspend fun getUserCommandChecks(): List<Check<*>>
}

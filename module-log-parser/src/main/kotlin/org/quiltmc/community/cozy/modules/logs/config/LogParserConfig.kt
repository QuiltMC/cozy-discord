/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.config

import dev.kord.core.event.Event
import dev.kordex.core.checks.types.Check
import org.quiltmc.community.cozy.modules.logs.types.BaseLogHandler
import org.quiltmc.community.cozy.modules.logs.types.LogParser
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor
import org.quiltmc.community.cozy.modules.logs.types.LogRetriever

public typealias Predicate = suspend (BaseLogHandler).(Event) -> Boolean

public interface LogParserConfig {
	/**
	 * Get the configured regular expression used to extract URLs from message content. This regex must only have
	 * one capturing group - if you need others, make them non-capturing.
	 */
	public suspend fun getUrlRegex(): Regex

	/**
	 * Get the configured log parsers, used to extract information from logs.
	 */
	public suspend fun getParsers(): List<LogParser>

	/**
	 * Get the configured log processors, used to action extracted log information.
	 */
	public suspend fun getProcessors(): List<LogProcessor>

	/**
	 * Get the configured log retrievers, used to locate and retrieve log content.
	 */
	public suspend fun getRetrievers(): List<LogRetriever>

	/**
	 * Get the configured staff command checks, used to ensure a staff-facing command can be run.
	 */
	public suspend fun getStaffCommandChecks(): List<Check<*>>

	/**
	 * Get the configured user command checks, used to ensure a user-facing command can be run.
	 */
	public suspend fun getUserCommandChecks(): List<Check<*>>

	/** Get the configured predicates that must pass (return `true`) for any handler to run. **/
	public suspend fun getGlobalPredicates(): List<Predicate>
}

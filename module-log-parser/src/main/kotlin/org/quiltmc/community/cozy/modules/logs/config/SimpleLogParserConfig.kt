/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import org.quiltmc.community.cozy.modules.logs.parsers.LauncherParser
import org.quiltmc.community.cozy.modules.logs.parsers.LoaderParser
import org.quiltmc.community.cozy.modules.logs.parsers.MinecraftVersionParser
import org.quiltmc.community.cozy.modules.logs.parsers.fabric.FabricModsParser
import org.quiltmc.community.cozy.modules.logs.parsers.launchers.ATLauncherParser
import org.quiltmc.community.cozy.modules.logs.parsers.launchers.MMCLikeParser
import org.quiltmc.community.cozy.modules.logs.parsers.launchers.TechnicParser
import org.quiltmc.community.cozy.modules.logs.parsers.quilt.QuiltModsParser
import org.quiltmc.community.cozy.modules.logs.processors.PlayerIPProcessor
import org.quiltmc.community.cozy.modules.logs.processors.quilt.FabricImplProcessor
import org.quiltmc.community.cozy.modules.logs.processors.quilt.IncompatibleModProcessor
import org.quiltmc.community.cozy.modules.logs.processors.quilt.QuiltLibrariesVersionProcessor
import org.quiltmc.community.cozy.modules.logs.processors.quilt.QuiltLoaderVersionProcessor
import org.quiltmc.community.cozy.modules.logs.retrievers.AttachmentLogRetriever
import org.quiltmc.community.cozy.modules.logs.retrievers.PastebinLogRetriever
import org.quiltmc.community.cozy.modules.logs.types.LogParser
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor
import org.quiltmc.community.cozy.modules.logs.types.LogRetriever

public class SimpleLogParserConfig(private val builder: Builder) : LogParserConfig {
	override suspend fun getParsers(): List<LogParser> = builder.parsers.sortedBy { it.order.value }
	override suspend fun getProcessors(): List<LogProcessor> = builder.processors.sortedBy { it.order.value }
	override suspend fun getRetrievers(): List<LogRetriever> = builder.retrievers.sortedBy { it.order.value }
	override suspend fun getUrlRegex(): Regex = builder.urlRegex

	override suspend fun getStaffCommandChecks(): List<Check<*>> = builder.staffCommandChecks
	override suspend fun getUserCommandChecks(): List<Check<*>> = builder.userCommandChecks
	override suspend fun getGlobalPredicates(): List<Predicate> = builder.globalPredicates

	public class Builder {
		public var parsers: MutableList<LogParser> = mutableListOf(
			FabricModsParser(),
			ATLauncherParser(),
			MMCLikeParser(),
			TechnicParser(),
			QuiltModsParser(),
			LauncherParser(),
			LoaderParser(),
			MinecraftVersionParser(),
		)

		public var processors: MutableList<LogProcessor> = mutableListOf(
			FabricImplProcessor(),
			IncompatibleModProcessor(),
			QuiltLibrariesVersionProcessor(),
			QuiltLoaderVersionProcessor(),
			PlayerIPProcessor(),
		)

		public var retrievers: MutableList<LogRetriever> = mutableListOf(
			AttachmentLogRetriever(),
			PastebinLogRetriever()
		)

		public var staffCommandChecks: MutableList<Check<*>> = mutableListOf()
		public var userCommandChecks: MutableList<Check<*>> = mutableListOf()
		public var globalPredicates: MutableList<Predicate> = mutableListOf()

		public var urlRegex: Regex = "(https?://[^\\s>]+)".toRegex(RegexOption.IGNORE_CASE)

		public fun parser(parser: LogParser): Boolean = parsers.add(parser)
		public fun processor(parser: LogProcessor): Boolean = processors.add(parser)
		public fun retriever(parser: LogRetriever): Boolean = retrievers.add(parser)
		public fun staffCommandCheck(check: Check<*>): Boolean = staffCommandChecks.add(check)
		public fun userCommandCheck(check: Check<*>): Boolean = userCommandChecks.add(check)
		public fun globalPredicate(predicate: Predicate): Boolean = globalPredicates.add(predicate)

		public fun build(): SimpleLogParserConfig = SimpleLogParserConfig(this)
	}
}

public inline fun SimpleLogParserConfig(builder: (SimpleLogParserConfig.Builder).() -> Unit): SimpleLogParserConfig {
	val builderObj = SimpleLogParserConfig.Builder()

	builder(builderObj)

	return builderObj.build()
}

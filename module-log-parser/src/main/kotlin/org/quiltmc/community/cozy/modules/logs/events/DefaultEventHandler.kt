/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.events

import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.extensions.event
import org.quiltmc.community.cozy.modules.logs.LogParserExtension

public class DefaultEventHandler(private val extension: LogParserExtension) : EventHandler {
	override suspend fun setup(): Unit = with(extension) {
		event<MessageCreateEvent> {
			action {
				handleMessage(event.message, event)
			}
		}
	}
}

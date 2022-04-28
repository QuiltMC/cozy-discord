/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.retrievers

import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.core.entity.Message
import java.nio.charset.Charset

class AttachmentLogRetriever : BaseLogRetriever {
    override suspend fun getLogContent(message: Message): List<String> =
        message.attachments.filter {
            it.filename.endsWith(".log") ||
                    it.filename.endsWith(".txt") ||
                    "." !in it.filename
        }.map {
            it.download().toString(Charset.forName("UTF-8"))
        }
}

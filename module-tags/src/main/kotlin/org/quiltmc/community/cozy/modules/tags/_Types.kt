/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("Filename")

package org.quiltmc.community.cozy.modules.tags

import dev.kord.rest.builder.message.create.MessageCreateBuilder
import org.quiltmc.community.cozy.modules.tags.data.Tag

/** Type alias representing a tag formatter callback. **/
public typealias TagFormatter = suspend MessageCreateBuilder.(tag: Tag) -> Unit

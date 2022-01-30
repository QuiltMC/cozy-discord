/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("Filename")

package org.quiltmc.community.cozy.modules.cleanup

import dev.kord.core.entity.Guild

public typealias GuildPredicate = suspend (guild: Guild) -> Boolean

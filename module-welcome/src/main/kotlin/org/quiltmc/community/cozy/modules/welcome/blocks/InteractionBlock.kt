/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.blocks

import dev.kord.core.event.interaction.InteractionCreateEvent

public interface InteractionBlock {
    public suspend fun handleInteraction(event: InteractionCreateEvent)
}

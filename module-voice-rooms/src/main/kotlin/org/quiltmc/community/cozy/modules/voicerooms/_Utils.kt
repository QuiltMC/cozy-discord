/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.voicerooms

public val Boolean.enabled: String
	get() = if (this) {
		"✅ Enabled"
	} else {
		"❌ Disabled"
	}

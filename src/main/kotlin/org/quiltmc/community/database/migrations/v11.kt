/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedParameter")
suspend fun v11(db: CoroutineDatabase) {
	// Suggestions no longer exist as a feature, so no migration is necessary.
}

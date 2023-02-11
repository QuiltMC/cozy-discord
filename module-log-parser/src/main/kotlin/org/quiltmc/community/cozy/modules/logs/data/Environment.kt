/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.data

// NOTE: Doesn't seem like it's possible to get any of this info reliably
public data class Environment(
	public var glInfo: String? = null,
	public var os: String? = null,
	public var javaVersion: String = "Unknown",
	public var jvmVersion: String = "Unknown",
	public var jvmArgs: String = "Unknown",
)

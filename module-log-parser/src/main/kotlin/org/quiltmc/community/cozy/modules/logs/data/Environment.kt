/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.data

// NOTE: Doesn't seem like it's possible to get any of this info reliably
@Suppress("DataClassShouldBeImmutable")
public data class Environment(
	public var cpu: String? = null,
	public var gpu: String? = null,

	public var gameMemory: String? = null,
	public var systemMemory: String? = null,
	public var shaderpack: String? = null,

	public var os: String? = null,
	public var javaVersion: String? = null,
	public var jvmVersion: String? = null,
	public var jvmArgs: String? = null,
)

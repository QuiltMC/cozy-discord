plugins {
	`api-module`
	`cozy-module`
	`published-module`
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.logging)

	implementation(platform(libs.kotlin.bom))
	implementation(libs.kotlin.stdlib)
}

kordEx {
	module("annotations")
	module("extra-pluralkit")
	module("unsafe")
}

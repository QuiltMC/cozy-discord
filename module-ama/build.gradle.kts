plugins {
    `api-module`
	`cozy-module`
	`published-module`
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	ksp(libs.kordex.annotationProcessor)

	implementation(libs.kordex.annotations)
	implementation(libs.kordex.core)
	implementation(libs.kordex.unsafe)
	implementation(libs.kordex.pluralkit)

	implementation(libs.logging)

	implementation(platform(libs.kotlin.bom))
	implementation(libs.kotlin.stdlib)
}

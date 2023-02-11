package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class DownloadTask : DefaultTask() {
	@Input
	lateinit var sourceUrl: String

	@OutputFile
	lateinit var target: File

	@TaskAction
	fun download() {
		ant.invokeMethod(
			"get",
			mapOf(
				"src" to sourceUrl,
				"dest" to target
			)
		)
	}
}

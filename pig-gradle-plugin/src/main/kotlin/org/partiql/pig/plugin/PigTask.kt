package org.partiql.pig.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class PigTask : DefaultTask() {

    init {
        group = "pig"
    }

    @get:Input
    @get:Option(
        option = "universe",
        description = "Type universe input file"
    )
    abstract val universe: Property<String>

    @get:Input
    @get:Option(
        option = "target",
        description = "Target language"
    )
    abstract val target: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "outputFile",
        description = "Generated output file (for targets that output a single file)"
    )
    abstract val outputFile: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "outputDir",
        description = "Generated output directory (for targets that output multiple files)"
    )
    abstract val outputDir: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "namespace",
        description = "Namespace for generated code"
    )
    abstract val namespace: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "template",
        description = "Path to an Apache FreeMarker template"
    )
    abstract val template: Property<String>

    @TaskAction
    fun action() {
        val args = mutableListOf<String>()
        // required args
        args += listOf("-u", universe.get())
        args += listOf("-t", target.get())
        // optional args
        if (outputFile.isPresent) {
            args += listOf("-o", outputFile.get())
        }
        if (outputDir.isPresent) {
            args += listOf("-d", outputDir.get())
        }
        if (namespace.isPresent) {
            args += listOf("-n", namespace.get())
        }
        if (template.isPresent) {
            args += listOf("-e", template.get())
        }
        // invoke pig compiler, offloads all arg handling to the application
        // also invoking via the public interface for consistency
        println("pig ${args.joinToString(" ")}")
        org.partiql.pig.main(args.toTypedArray())
    }
}

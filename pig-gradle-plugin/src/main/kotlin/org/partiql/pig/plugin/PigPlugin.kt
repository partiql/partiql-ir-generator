package org.partiql.pig.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

abstract class PigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Ensure `sourceSets` extension exists
        project.pluginManager.apply(JavaPlugin::class.java)

        // Adds pig source set extension to all source sets
        project.sourceSets().forEach { sourceSet ->
            val name = sourceSet.name
            val sds = project.objects.sourceDirectorySet(name, "$name PIG source")
            sds.srcDir("src/$name/pig")
            sds.include("**/*.ion")
            sourceSet.extensions.add("pig", sds)
        }

        // Extensions for pig compiler arguments
        val ext = project.extensions.create("pig", PigExtension::class.java, project)

        // Create tasks after source sets have been evaluated
        project.afterEvaluate {
            project.sourceSets().forEach { sourceSet ->
                // Pig generate all for the given source set
                val pigAllTaskName = getPigAllTaskName(sourceSet)
                val pigAllTask = project.tasks.create(pigAllTaskName) {
                    it.group = "pig"
                    it.description = "Generate all PIG sources for ${sourceSet.name} source set"
                }

                // If outDir is conventional, add generated sources to javac sources
                // Else you're responsible for your own configuration choices
                var outDir = ext.outputDir.get()
                if (outDir == ext.conventionalOutDir) {
                    outDir = outDir + "/" + sourceSet.name
                    sourceSet.java.srcDir(outDir)
                }

                // Create a pig task for each type universe and each source set
                (sourceSet.extensions.getByName("pig") as SourceDirectorySet).files.forEach { file ->
                    val universeName = file.name.removeSuffix(".ion").lowerToCamelCase().capitalize()
                    val pigTask = project.tasks.create(pigAllTaskName + universeName, PigTask::class.java) { task ->
                        task.description = "Generated PIG sources for $universeName"
                        task.universe.set(file.absolutePath)
                        task.target.set(ext.target)
                        task.outputDir.set(outDir)
                        task.outputFile.set(ext.outputFile)
                        task.namespace.set(ext.namespace)
                        task.template.set(ext.template)
                    }
                    pigAllTask.dependsOn(pigTask)
                }

                // Execute pig tasks before compiling
                project.tasks.named(sourceSet.compileJavaTaskName) {
                    it.dependsOn(pigAllTask)
                }
            }
        }
    }

    private fun Project.sourceSets(): List<SourceSet> = extensions.getByType(SourceSetContainer::class.java).toList()

    private fun getPigAllTaskName(sourceSet: SourceSet) = when (sourceSet.name) {
        "main" -> "generatePigSource"
        else -> "generatePig${sourceSet.name.capitalize()}Source"
    }

    /**
     * Type Universe files are lower hyphen, but Gradle tasks are lower camel
     */
    private fun String.lowerToCamelCase(): String =
        this.split('-')
            .filter { it.isNotEmpty() }
            .mapIndexed { i, str ->
                when (i) {
                    0 -> str
                    else -> str.capitalize()
                }
            }
            .joinToString(separator = "")
}

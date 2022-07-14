package org.partiql.pig.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PigExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    val conventionalOutDir: String

    init {
        conventionalOutDir = "${project.buildDir}/generated-sources/pig"
    }

    // required
    val target: Property<String> = objects.property(String::class.java)

    // optional
    val outputFile: Property<String> = objects.property(String::class.java)

    // optional
    val outputDir: Property<String> = objects
        .property(String::class.java)
        .convention(conventionalOutDir)

    // optional
    val namespace: Property<String> = objects.property(String::class.java)

    // optional
    val template: Property<String> = objects.property(String::class.java)
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    id(Plugins.conventions)
    id(Plugins.application)
    id(Plugins.publish)
}

dependencies {
    implementation(Deps.dotlin)
    implementation(Deps.freemarker)
    implementation(Deps.ionElement)
    implementation(Deps.kasechange)
    implementation(Deps.kotlinPoet)
    implementation(Deps.picoCli)
}

application {
    applicationName = "pig"
    mainClass.set("org.partiql.pig.PigKt")
}

distributions {
    main {
        distributionBaseName.set("pig")
    }
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}

publish {
    artifactId = "pig"
    name = "PartiQL I.R. Generator (a.k.a P.I.G.)"
}

val generatedVersion = "$buildDir/generated-version"

sourceSets {
    main {
        output.dir(generatedVersion)
    }
}

tasks.processResources {
    dependsOn(tasks.findByName("generateVersionAndHash"))
}

tasks.create("generateVersionAndHash") {
    val propertiesFile = file("$generatedVersion/pig.properties")
    propertiesFile.parentFile.mkdirs()
    val properties = Properties()
    // Version
    val version = version.toString()
    properties.setProperty("version", version)
    // Commit Hash
    val commit = ByteArrayOutputStream().apply {
        exec {
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = this@apply
        }
    }.toString().trim()
    properties.setProperty("commit", commit)
    // Write file
    val out = FileOutputStream(propertiesFile)
    properties.store(out, null)
}

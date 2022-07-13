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
import java.io.FileOutputStream
import java.util.Properties

plugins {
    id("pig.conventions")
    id("pig.publish")
    id("application")
}

project.description = "The P.I.G. is a code generator for domain models such ASTs and execution plans."

val propertiesDir = "$buildDir/properties"

dependencies {
    implementation("org.freemarker:freemarker:2.3.30")
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("com.amazon.ion:ion-element:0.2.0")
}

application {
    mainClass.set("org.partiql.pig.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.partiql.pig.MainKt"
    }
    from(
        configurations.compile.get().map {
            if (it.isDirectory) {
                it
            } else {
                zipTree(it)
            }
        }
    )
}

tasks.register("generateProperties") {
    doLast {
        val propertiesFile = file("$propertiesDir/pig.properties")
        propertiesFile.parentFile.mkdirs()
        val properties = Properties()
        properties.setProperty("version", version.toString())
        val out = FileOutputStream(propertiesFile)
        properties.store(out, null)
    }
}

tasks.named("processResources") {
    dependsOn("generateProperties")
}

sourceSets {
    main {
        output.dir(propertiesDir)
    }
}

tasks.build {
    finalizedBy(tasks.installDist)
}

import java.nio.file.Paths

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

plugins {
    id("pig.conventions")
    id("java-library")
}

dependencies {
    implementation(project(":pig-runtime"))
}

// remove once pig-gradle-plugin is updated
tasks {

    val pigOutputDir = "src/main/kotlin/org/partiql/pig/legacy/tests/generated/"
    val pigOutputPackage = "org.partiql.pig.legacy.tests.generated"
    val pigInputDir = "src/test/pig/"
    val universes = listOf("toy-lang", "sample-universe", "partiql-basic")

    val pigClean = register("pig-clean", Delete::class) {
        group = "pig"
        description = "deletes all pig generated files"
        delete(
            fileTree(pigOutputDir).matching {
                include("*.generated.kt")
            }
        )
    }

    val pigAll = register("pig-all") {
        group = "pig"
        description = "run all pig generation tasks"
        shouldRunAfter(pigClean)
    }

    named("compileKotlin") {
        dependsOn(pigAll)
    }

    // :pig:installDist creates the pig jar launch script required for `exec`
    val installDist = project(":pig").tasks.named("install")

    universes.forEach { u ->
        val t = register("pig-generate-$u") {
            group = "pig"
            description = "pig generation for type universe $u"
            dependsOn(installDist)

            // pig script from :pig:install
            val pathToPig = File(projectDir, "../pig/build/install/pig/bin/pig").canonicalPath
            val pathToUniverse = Paths.get(projectDir.toString(), pigInputDir, "$u.ion")

            doLast {
                exec {
                    workingDir = projectDir
                    commandLine(
                        pathToPig,
                        "legacy", // invoke 0.x
                        "-u", pathToUniverse,
                        "-t", "kotlin",
                        "-n", pigOutputPackage,
                        "-d", pigOutputDir
                    )
                }
            }
        }
        pigAll {
            dependsOn(t)
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/*.generated.kt")
    }
}

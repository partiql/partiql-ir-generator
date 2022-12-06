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
    id("org.partiql.pig.pig-gradle-plugin")
    id("java-library")
}

dependencies {
    implementation(project(":pig-runtime"))
}

// remove after pig-example is created
val pigOutputDir = file("./src/main/kotlin/org/partiql/pig/tests/generated/")

pig {
    namespace = "org.partiql.pig.tests.generated"
    // remove after pig-example is created
    outputDir = pigOutputDir
}

// remove after pig-example is created
tasks.register("pigClean", Delete::class) {
    group = "pig"
    description = "deletes all pig generated files"
    delete(
        fileTree(pigOutputDir).matching {
            include("*.generated.kt")
        }
    )
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/*.generated.kt")
    }
}

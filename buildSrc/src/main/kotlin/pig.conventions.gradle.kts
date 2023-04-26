import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jlleitschuh.gradle.ktlint.KtlintExtension

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
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val buildDir = File(rootProject.projectDir, "gradle-build/" + project.name)

dependencies {
    implementation(Deps.kotlin)
    testImplementation(Deps.kotlinTest)
    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.jupiter)
}

val generatedSrc = "$buildDir/generated-src"

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.javaTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.javaTarget)
}

tasks.test {
    useJUnitPlatform() // Enable JUnit5
    jvmArgs.addAll(listOf("-Duser.language=en", "-Duser.country=US"))
    maxHeapSize = "4g"
    testLogging {
        events.add(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
    dependsOn(tasks.ktlintCheck)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.javaTarget
    kotlinOptions.apiVersion = Versions.kotlinTarget
    kotlinOptions.languageVersion = Versions.kotlinTarget
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.javaTarget
    kotlinOptions.apiVersion = Versions.kotlinTarget
    kotlinOptions.languageVersion = Versions.kotlinTarget
}

configure<KtlintExtension> {
    filter {
        exclude { it.file.path.contains(generatedSrc) }
    }
}

sourceSets {
    main {
        java.srcDir(generatedSrc)
    }
}

kotlin.sourceSets {
    all {
        // languageSettings.optIn("kotlin.RequiresOptIn")
    }
    main {
        kotlin.srcDir(generatedSrc)
    }
}

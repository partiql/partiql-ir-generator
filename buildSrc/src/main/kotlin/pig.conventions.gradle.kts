import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.random.RandomGeneratorFactory.all

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

object Versions {
    // Language
    const val kotlin = "1.5.31"
    const val kotlinTarget = "1.4"
    const val javaTarget = "1.8"

    // Dependencies
    const val ionJava = "1.9.4"
    const val ionElement = "1.0.0"
    const val jline = "3.21.0"
    const val kasechange = "1.3.0"
    const val kotlinPoet = "1.8.0" // Kotlin 1.5
    const val picoCli = "4.7.0"
    // Testing
}

object Deps {
    // Language
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"

    // Dependencies
    const val ionElement = "com.amazon.ion:ion-element:${Versions.ionElement}"
    const val kasechange = "net.pearx.kasechange:kasechange:${Versions.kasechange}"
    const val kotlinPoet = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val picoCli = "info.picocli:picocli:${Versions.picoCli}"

    // Testing
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlin}"
}

object Plugins {
    // PIG
    const val conventions = "pig.conventions"
    const val publish = "org.partiql.pig.gradle.plugin.publish"

    // 3P
    const val application = "org.gradle.application"
    const val detekt = "io.gitlab.arturbosch.detekt"
    const val dokka = "org.jetbrains.dokka"
    const val ktlint = "org.jlleitschuh.gradle.ktlint"
    const val library = "org.gradle.java-library"
}

val buildDir = File(rootProject.projectDir, "gradle-build/" + project.name)

dependencies {
    implementation(Deps.kotlin)
    testImplementation(Deps.kotlinTest)
    testImplementation(Deps.kotlinTestJunit)
}

val generatedSrc = "$buildDir/generated-src"
val generatedVersion = "$buildDir/generated-version"

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.javaTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.javaTarget)
}

tasks.test {
    useJUnitPlatform() // Enable JUnit5
    jvmArgs!!.addAll(listOf("-Duser.language=en", "-Duser.country=US"))
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
        output.dir(generatedVersion)
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

tasks.processResources {
    dependsOn(tasks.findByName("generateVersionAndHash"))
}

tasks.create("generateVersionAndHash") {
    val propertiesFile = file("$generatedVersion/partiql.properties")
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

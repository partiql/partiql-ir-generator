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

object Versions {
    // Language
    const val kotlin = "1.5.31"
    const val kotlinTarget = "1.4"
    const val javaTarget = "1.8"
    // Dependencies
    const val dotlin = "1.0.2"
    const val freemarker = "2.3.30"
    const val ionJava = "1.9.4"
    const val ionElement = "1.0.0"
    const val jline = "3.21.0"
    const val kasechange = "1.3.0"
    const val kotlinPoet = "1.8.0" // Kotlin 1.5
    const val picoCli = "4.7.0"
    // Testing
    const val jupiter = "5.6.2"
}

object Deps {
    // Language
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    // Dependencies
    const val dotlin = "io.github.rchowell:dotlin:${Versions.dotlin}"
    const val ionElement = "com.amazon.ion:ion-element:${Versions.ionElement}"
    const val freemarker = "org.freemarker:freemarker:${Versions.freemarker}"
    const val kasechange = "net.pearx.kasechange:kasechange:${Versions.kasechange}"
    const val kotlinPoet = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val picoCli = "info.picocli:picocli:${Versions.picoCli}"
    // Testing
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlin}"
    const val jupiter = "org.junit.jupiter:junit-jupiter:${Versions.jupiter}"
}

object Plugins {
    // PIG
    const val conventions = "pig.conventions"
    const val publish = "org.partiql.pig.gradle.publish"
    // 3P
    const val application = "org.gradle.application"
    const val detekt = "io.gitlab.arturbosch.detekt"
    const val dokka = "org.jetbrains.dokka"
    const val ktlint = "org.jlleitschuh.gradle.ktlint"
    const val library = "org.gradle.java-library"
}

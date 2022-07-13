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
    val kotlin = "1.4.0"
    val ion = "1.9.4"
    val jupiter = "5.6.2"
    val jvmTarget = JavaVersion.VERSION_1_8
}

val buildDir = File(rootProject.projectDir, "gradle-build/" + project.name)

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.jupiter}")
}

java {
    sourceCompatibility = Versions.jvmTarget
    targetCompatibility = Versions.jvmTarget
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget.toString()
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget.toString()
}

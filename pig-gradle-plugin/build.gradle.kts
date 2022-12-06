plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "1.4.0"
    id("com.gradle.plugin-publish") version "1.1.0"
}

repositories {
    mavenCentral()
}

object Versions {
    const val pig = "0.6.1"
    const val kotlinTarget = "1.4"
    const val javaTarget = "1.8"
}

// latest maven central release
version = Versions.pig
group = "org.partiql"

dependencies {
    implementation("org.partiql:partiql-ir-generator:${Versions.pig}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

pluginBundle {
    website = "https://github.com/partiql/partiql-ir-generator/wiki"
    vcsUrl = "https://github.com/partiql/partiql-ir-generator"
    tags = listOf("partiql", "pig", "ir", "partiql-ir-generator")
}

gradlePlugin {
    plugins {
        create("pig-gradle-plugin") {
            id = "org.partiql.pig.pig-gradle-plugin"
            displayName = "PIG Gradle Plugin"
            description = "The PIG gradle plugin exposes a Gradle task to generate sources from a PIG type universe"
            implementationClass = "org.partiql.pig.gradle.PigPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.javaTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.javaTarget)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.javaTarget
    kotlinOptions.apiVersion = Versions.kotlinTarget
    kotlinOptions.languageVersion = Versions.kotlinTarget
}

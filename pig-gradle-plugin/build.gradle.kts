plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "1.4.0"
    id("com.gradle.plugin-publish") version "1.0.0"
}

repositories {
    mavenCentral()
}

version = "0.5.1-SNAPSHOT"
group = "org.partiql"

dependencies {
    // It is non-trivial to depend on a local plugin within a gradle project
    // The simplest way is using a composite build: https://docs.gradle.org/current/userguide/composite_builds.html
    // Other methods involved adding the build/lib/... jar to classpath, or publish to maven local
    // By adding the plugin as a dep in `pig-tests`, I cannot use an included build of `pig` in the plugin
    // Hence it's much simpler to use the latest published version in the plugin
    implementation("org.partiql:partiql-ir-generator:0.5.0")
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
            id = "pig-gradle-plugin"
            displayName = "PIG Gradle Plugin"
            description = "The PIG gradle plugin exposes a Gradle task to generate sources from a PIG type universe"
            implementationClass = "org.partiql.pig.plugin.PigPlugin"
        }
    }
}

//
// // TODO https://github.com/partiql/partiql-ir-generator/issues/132
// publishing {
//    repositories {
//        maven {
//            name = 'mavenLocalPlugin'
//            url = '../maven-local-plugin'
//        }
//    }
// }

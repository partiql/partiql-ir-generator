package org.partiql.pig.gradle.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import java.io.File

/**
 * Gradle plugin to consolidates the following publishing logic
 * - Maven Publishing
 * - Signing
 * - SourcesJar
 * - Dokka + JavadocJar
 */
abstract class PigPublishPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(JavaPlugin::class.java)
        pluginManager.apply(MavenPublishPlugin::class.java)
        pluginManager.apply(SigningPlugin::class.java)
        pluginManager.apply(DokkaPlugin::class.java)
        val ext = extensions.create("publish", PublishExtension::class.java)
        target.afterEvaluate { publish(ext) }
    }

    private fun Project.publish(ext: PublishExtension) {
        val releaseVersion = !version.toString().endsWith("-SNAPSHOT")

        // Run dokka unless the environment explicitly specifies false
        val runDokka = (System.getenv()["DOKKA"] != "false") || releaseVersion

        // Include "sources" and "javadoc" in the JAR
        extensions.getByType(JavaPluginExtension::class.java).run {
            withSourcesJar()
            withJavadocJar()
        }

        tasks.getByName<DokkaTask>("dokkaHtml") {
            onlyIf { runDokka }
            outputDirectory.set(File("${buildDir}/javadoc"))
        }

        // Add dokkaHtml output to the javadocJar
        tasks.getByName<Jar>("javadocJar") {
            onlyIf { runDokka }
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            archiveClassifier.set("javadoc")
            from(tasks.named("dokkaHtml"))
        }

        // Setup Maven Central Publishing
        val publishing = extensions.getByType(PublishingExtension::class.java).apply {
            publications {
                create<MavenPublication>("maven") {
                    artifactId = ext.artifactId
                    from(components["java"])
                    pom {
                        packaging = "jar"
                        name.set(ext.name)
                        description.set("The P.I.G. is a code generator for domain models such ASTs and execution plans.")
                        url.set("git@github.com:partiql/partiql-ir-generator.git")
                        scm {
                            connection.set("scm:git@github.com:partiql/partiql-ir-generator.git")
                            developerConnection.set("scm:git@github.com:partiql/partiql-ir-generator.git")
                            url.set("git@github.com:partiql/partiql-ir-generator.git")
                        }
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                name.set("PartiQL Team")
                                email.set("partiql-dev@amazon.com")
                                organization.set("PartiQL")
                                organizationUrl.set("https://github.com/partiql")
                            }
                        }
                    }
                }
            }
            repositories {
                maven {
                    url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials {
                        val ossrhUsername: String by rootProject
                        val ossrhPassword: String by rootProject
                        username = ossrhUsername
                        password = ossrhPassword
                    }
                }
            }
        }

        // Sign only if publishing to Maven Central
        extensions.getByType(SigningExtension::class.java).run {
            setRequired {
                releaseVersion && gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
            }
            sign(publishing.publications["maven"])
        }
    }
}

abstract class PublishExtension {
    var artifactId: String = ""
    var name: String = ""
}

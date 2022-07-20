import gradle.kotlin.dsl.accessors._4ba0e6f3f9855dd979d4ade2b5984a53.java

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
    `maven-publish`
    id("signing")
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>(name) {
            artifactId = name.replace("pig", "partiql-ir-generator")
            from(components["java"])
            pom {
                url.set("https://partiql.org/")
                packaging = "jar"
                scm {
                    connection.set("scm:git@github.com:partiql/partiql-ir-generator.git")
                    developerConnection.set("scm:git@github.com:partiql/partiql-ir-generator.git")
                    url.set("git@github.com:partiql/partiql-ir-generator.git")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            name.set("PartiQL Team")
                            email.set("partiql-team@amazon.com")
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
}

val isReleaseVersion = !rootProject.version.toString().endsWith("SNAPSHOT")

signing {
    sign(publishing.publications[name])
    isRequired = isReleaseVersion
    // TODO figure out how to enable this
    // && gradle.taskGraph.hasTask("publish")
}

tasks.withType(Sign::class) {
    onlyIf {
        isReleaseVersion
    }
}

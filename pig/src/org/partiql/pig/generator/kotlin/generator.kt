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

package org.partiql.pig.generator.kotlin

import freemarker.template.Configuration
import org.partiql.pig.generator.createDefaultFreeMarkerConfiguration
import org.partiql.pig.generator.setClassLoaderForTemplates
import java.io.File
import java.io.PrintWriter
import java.time.OffsetDateTime

const val KOTLIN_SOURCE_SUFFIX = "generated.kt"

fun generateKotlinCode(
    namespace: String,
    kotlinTypeUniverse: KTypeUniverse,
    outputDirectory: File
) {
    val freemarkerConfig: Configuration = createDefaultFreeMarkerConfiguration()

    // Allow .getTemplate below and [#include...] directives to look in this .jar's embedded resources.
    freemarkerConfig.setClassLoaderForTemplates()

    generateDomainFiles(freemarkerConfig, kotlinTypeUniverse, namespace, outputDirectory)
    generateCrossDomainVisitorTransforms(freemarkerConfig, kotlinTypeUniverse.transforms, namespace, outputDirectory)
}

private fun generateDomainFiles(
    freemarkerConfig: Configuration,
    kotlinTypeUniverse: KTypeUniverse,
    namespace: String,
    outputDirectory: File
) {
    // Load the template
    val template = freemarkerConfig.getTemplate("kotlin-domain.ftl")!!

    // Apply the kotlin template once for each type domain... this creates one `*.generated.kt` file per domain.
    kotlinTypeUniverse.domains.forEach { domain ->
        val renderModel = KotlinDomainFreeMarkerGlobals(
            namespace = namespace,
            domain = domain,
            generatedDate = OffsetDateTime.now()
        )

        val outputFile = File(outputDirectory, "${domain.kotlinName}.$KOTLIN_SOURCE_SUFFIX")

        PrintWriter(outputFile).use { printWriter ->
            template.process(renderModel, printWriter)
        }
    }
}

private fun generateCrossDomainVisitorTransforms(
    freemarkerConfig: Configuration,
    transforms: List<KTransform>,
    namespace: String,
    outputDirectory: File
) {
    // Load the template
    val template = freemarkerConfig.getTemplate("kotlin-cross-domain-transform.ftl")!!

    transforms.forEach { transform ->
   // Apply the kotlin template once for each type domain... this creates one `*.generated.kt` file per domain.
        val renderModel = KotlinCrossDomainFreeMarkerGlobals(
            namespace = namespace,
            transform = transform,
            generatedDate = OffsetDateTime.now()
        )

        val outputFile = File(
            outputDirectory,
            "${transform.sourceDomainDifference.kotlinName}To${transform.destDomainKotlinName}.$KOTLIN_SOURCE_SUFFIX"
        )

        PrintWriter(outputFile).use { printWriter ->
            template.process(renderModel, printWriter)
        }
    }
}

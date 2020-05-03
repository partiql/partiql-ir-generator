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

package org.partiql.pig.generator.custom

import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.generator.createDefaultFreeMarkerConfiguration
import java.io.File
import java.io.PrintWriter
import java.time.OffsetDateTime

fun applyCustomTemplate(
    templateFile: File,
    domains: List<TypeDomain>,
    output: PrintWriter
) {
    val renderModel = createCustomFreeMarkerGlobals(domains)

    val cfg = createDefaultFreeMarkerConfiguration()

    cfg.setDirectoryForTemplateLoading(templateFile.parentFile)
    val template = cfg.getTemplate(templateFile.name)!!

    template.process(renderModel, output)
}

internal fun createCustomFreeMarkerGlobals(domains: List<TypeDomain>): CustomFreeMarkerGlobals =
    CustomFreeMarkerGlobals(
        domains = domains.map { it.toCTypeDomain() },
        generatedDate = OffsetDateTime.now())

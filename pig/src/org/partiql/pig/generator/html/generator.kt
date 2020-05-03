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

package org.partiql.pig.generator.html

import freemarker.template.Template
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.generator.createDefaultFreeMarkerConfiguration
import org.partiql.pig.generator.custom.createCustomFreeMarkerGlobals
import org.partiql.pig.generator.setClassLoaderForTemplates
import java.io.PrintWriter


fun applyHtmlTemplate(
    domains: List<TypeDomain>,
    output: PrintWriter
) {
    // Html uses the same model as the custom target
    val renderModel = createCustomFreeMarkerGlobals(domains)

    createHtmlTemplate().process(renderModel, output)
}

private fun createHtmlTemplate(): Template {
    val cfg = createDefaultFreeMarkerConfiguration()
    cfg.setClassLoaderForTemplates()
    return cfg.getTemplate("html.ftl")!!
}

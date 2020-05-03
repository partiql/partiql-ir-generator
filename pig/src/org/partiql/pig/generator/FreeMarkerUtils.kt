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

package org.partiql.pig.generator

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.partiql.pig.generator.kotlin.KotlinFreeMarkerGlobals


/**
 * When applying a template for a pre-packaged language target such as Kotlin, we
 * call this to load the templates from resources.  The `custom` language target
 * loads the templates from the file specified on the command-line.
 */
internal fun Configuration.setClassLoaderForTemplates() {
    // Specify the source where the template files come from. Here we set a
    // the classloader of [TopLevelFreeMarkerGlobals] and set the root package.
    this.setClassLoaderForTemplateLoading(
        KotlinFreeMarkerGlobals::class.java.classLoader,
        "/org/partiql/pig/templates")
}


/**
 * Creates the default Apache FreeMarker [Configuration] object that should be used
 * when applying any template.
 */
internal fun createDefaultFreeMarkerConfiguration(): Configuration {
    // Create your Configuration instance, and specify if up to what FreeMarker
    // version (here 2.3.27) do you want to apply the fixes that are not 100%
    // backward-compatible. See the Configuration JavaDoc for details.
    val cfg = Configuration(Configuration.VERSION_2_3_27)

    // Set the preferred charset template files are stored in. UTF-8 is
    // a good choice in most applications:
    cfg.defaultEncoding = "UTF-8"

    // Sets how errors will appear.
    // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
    cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER

    // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
    cfg.logTemplateExceptions = false

    // Wrap unchecked exceptions thrown during template processing into TemplateException-s.
    cfg.wrapUncheckedExceptions = true

    cfg.tagSyntax = Configuration.SQUARE_BRACKET_TAG_SYNTAX

    // Exposes the `indent` directive we use to make indenting parts of the result easier.
    cfg.setSharedVariable("indent", IndentDirective())
    return cfg
}



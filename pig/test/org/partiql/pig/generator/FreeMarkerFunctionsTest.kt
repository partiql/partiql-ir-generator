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

import freemarker.cache.StringTemplateLoader
import org.junit.jupiter.api.Test
import java.io.StringWriter
import kotlin.test.assertEquals

/**
 * A couple of tests to ensure that the functions we expose to all freemarker templates
 * actually work.  These are exposed to templates for all language targets.
 */
class FreeMarkerFunctionsTest {

    private fun applyDummyTemplate(template: String): String {
        val cfg = createDefaultFreeMarkerConfiguration()
        val loader = StringTemplateLoader()
        loader.putTemplate("dummyTemplate", template)
        cfg.templateLoader = loader

        val t = cfg.getTemplate("dummyTemplate")
        val sw = StringWriter()
        t.process(null, sw)
        return sw.toString()
    }

    @Test
    fun snakeToCamelCase() {
        // ${'$'} inserts a literal $ in the string
        assertEquals("fooBar", applyDummyTemplate("""${'$'}{snakeToCamelCase("foo_bar")}"""))
    }

    @Test
    fun snakeToPascalCase() {
        // ${'$'} inserts a literal $ in the string
        assertEquals("FooBar", applyDummyTemplate("""${'$'}{snakeToPascalCase("foo_bar")}"""))
    }
}

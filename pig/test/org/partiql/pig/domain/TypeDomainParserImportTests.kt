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

package org.partiql.pig.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.domain.parser.parseTypeUniverseFile
import kotlin.test.assertTrue

class TypeDomainParserImportTests {

    @Test
    fun testImport() {
        val universe = parseTypeUniverseFile("test-domains/main.ion")
        val allDomains = universe.statements.filterIsInstance<TypeDomain>()

        // If 4 domains are loaded correctly, then we deal with relative paths and circular references correctly.
        // see documentation at top of test-domains/main.ion
        Assertions.assertEquals(4, allDomains.size)
        assertTrue(allDomains.any { it.tag == "domain_a" })
        assertTrue(allDomains.any { it.tag == "domain_b" })
        assertTrue(allDomains.any { it.tag == "domain_c" })
        assertTrue(allDomains.any { it.tag == "domain_d" })
    }
}

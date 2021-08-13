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

package org.partiql.pig.domain.parser

import com.amazon.ionelement.api.IonTextLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.errors.PigError
import org.partiql.pig.errors.PigException
import org.partiql.pig.util.MAIN_DOMAINS_DIR
import org.partiql.pig.util.ROOT_A
import org.partiql.pig.util.ROOT_B
import org.partiql.pig.util.parseWithTestRoots
import java.nio.file.Paths

class TypeDomainParserIncludeFileTests {
    @Test
    fun `include happy case`() {
        val universe = parseWithTestRoots("test-domains/main.ion")
        val allDomains = universe.statements.filterIsInstance<TypeDomain>()

        // If the following 6 domains are loaded, then we deal with relative paths and circular references correctly.
        // see documentation at top of test-domains/main.ion
        assertEquals(
            setOf(
                "domain_a",
                "domain_b",
                "domain_c",
                "domain_d",
                "domain_f",
                "domain_s"
            ),
            allDomains.map { it.tag }.toSet()
        )
    }



    @Test
    fun `include sad case - missing include - absolute path`() {
        val includeeFile = "dir_x/does-not-exist.ion"
        testMissingInclude(
            mainUniverseFile = "test-domains/include-missing.ion",
            includeeFile = includeeFile,
            pathsSearched = listOf(
                "${MAIN_DOMAINS_DIR}/$includeeFile",
                "${ROOT_A.resolve("dir_x/does-not-exist.ion").toAbsolutePath()}",
                "${ROOT_B.resolve("dir_x/does-not-exist.ion").toAbsolutePath()}"
            )
        )
    }

    private fun testMissingInclude(
        mainUniverseFile: String,
        includeeFile: String,
        pathsSearched: List<String>
    ) {
        val ex = assertThrows<PigException> { parseWithTestRoots(mainUniverseFile) }
        assertEquals(
            PigError(
                SourceLocation(Paths.get(mainUniverseFile).toAbsolutePath().toString(), IonTextLocation(4L, 1L)),
                ParserErrorContext.IncludeFileNotFound(
                    includeeFile,
                    pathsSearched
                )
            ),
            ex.error
        )
    }
}

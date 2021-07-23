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

import com.amazon.ionelement.api.IonTextLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.ParserErrorContext
import org.partiql.pig.domain.parser.SourceLocation
import org.partiql.pig.domain.parser.parseMainTypeUniverse
import org.partiql.pig.errors.PigError
import org.partiql.pig.errors.PigException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertTrue

class TypeDomainParserIncludeFileTests {
    private val mainDir = Paths.get("test-domains").toAbsolutePath()
    private val rootA = mainDir.resolve("root_a").toAbsolutePath()
    private val rootB = mainDir.resolve("root_b").toAbsolutePath()

    @Test
    fun `include happy case`() {
        val universe = parseWithTestRoots("test-domains/main.ion")
        val allDomains = universe.statements.filterIsInstance<TypeDomain>()

        // If 5 domains are loaded correctly, then we deal with relative paths and circular references correctly.
        // see documentation at top of test-domains/main.ion
        assertEquals(5, allDomains.size)
        assertTrue(allDomains.any { it.tag == "domain_a" })
        assertTrue(allDomains.any { it.tag == "domain_b" })
        assertTrue(allDomains.any { it.tag == "domain_c" })
        assertTrue(allDomains.any { it.tag == "domain_d" })
        assertTrue(allDomains.any { it.tag == "domain_s" })
    }

    private fun parseWithTestRoots(universeFile: String): TypeUniverse {
        val includeSearchRoots = listOf(rootA, rootB)
        return parseMainTypeUniverse(Paths.get(universeFile), includeSearchRoots)
    }

    @Test
    fun `include sad case - missing include - relative path`() {
        val includeeFile = "does-not-exist.ion"
        testMissingInclude(
            mainUniverseFile = "test-domains/include-missing-relative.ion",
            includeeFile = includeeFile,
            pathsSearched = listOf(
                "${mainDir}/$includeeFile",
                "${rootA.resolve("does-not-exist.ion").toAbsolutePath()}",
                "${rootB.resolve("does-not-exist.ion").toAbsolutePath()}"
            )
        )
    }

    @Test
    fun `include sad case - missing include - absolute path`() {
        val includeeFile = "/dir_x/does-not-exist.ion"
        testMissingInclude(
            mainUniverseFile = "test-domains/include-missing-absolute.ion",
            includeeFile = includeeFile,
            pathsSearched = listOf(
                "${mainDir}$includeeFile",
                "${rootA.resolve("dir_x/does-not-exist.ion").toAbsolutePath()}",
                "${rootB.resolve("dir_x/does-not-exist.ion").toAbsolutePath()}"
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

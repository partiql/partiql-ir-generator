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

package org.partiql.pig.include

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.domain.include.IncludeResolver
import org.partiql.pig.domain.include.IncludeResolutionException
import org.partiql.pig.domain.include.InvalidIncludePathException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

class IncludeResolverTests {
    companion object {
        private val PATH_TO_MAIN = Paths.get("./test-domains/main.ion")

        class HappyCase(val includeePath: String, expectedResolved: String) {
            val includerPath: Path = PATH_TO_MAIN
            val expectedResolvedPath: Path = Paths.get(expectedResolved).toAbsolutePath().normalize()
        }

        @JvmStatic @Suppress("UNUSED")
        fun happyCases() = listOf(
            HappyCase(
                includeePath = "sibling-of-main.ion",
                expectedResolved = "./test-domains/sibling-of-main.ion"
            ),
            HappyCase(
                includeePath = "/dir_x/universe_a.ion",
                expectedResolved = "./test-domains/root_a/dir_x/universe_a.ion"
            ),
            HappyCase(
                includeePath = "/dir_z/universe_b.ion",
                expectedResolved = "./test-domains/root_b/dir_z/universe_b.ion"
            )
        )

        class NotFoundCase(includee: String, val expectedConsdieredPaths: List<String>) {
            val includee: Path = Paths.get(includee)
        }

        @JvmStatic @Suppress("UNUSED")
        fun notFoundCases() = listOf(
            NotFoundCase(
                includee = "does-not-exist.ion",
                expectedConsdieredPaths = listOf(
                    // no / at start of includePath, therefore we include the parent directory of the includer
                    Paths.get("test-domains/does-not-exist.ion").toAbsolutePath().toString(),
                    Paths.get("test-domains/root_a/does-not-exist.ion").toAbsolutePath().toString(),
                    Paths.get("test-domains/root_b/does-not-exist.ion").toAbsolutePath().toString()
                )
            ),
            NotFoundCase(
                includee = "/does-not-exist.ion",
                expectedConsdieredPaths = listOf(
                    // / at start of includePath, therefore we exclude the parent directory of the includer.
                    Paths.get("test-domains/root_a/does-not-exist.ion").toAbsolutePath().toString(),
                    Paths.get("test-domains/root_b/does-not-exist.ion").toAbsolutePath().toString())
                )
            )
    }

    private val resolver = IncludeResolver(
        listOf(
            Paths.get("./test-domains/root_a"),
            Paths.get("./test-domains/root_b")
        ),
        // These tests refer to actual files in the source tree--here we should use the default FileSystem.
        FileSystems.getDefault()
    )

    @ParameterizedTest
    @MethodSource("happyCases")
    fun `test happy resolution cases`(tc: HappyCase) {
        val actualResolvedPath = resolver.resolve(Paths.get(tc.includeePath), tc.includerPath)
        assertEquals(tc.expectedResolvedPath, actualResolvedPath)
    }

    @ParameterizedTest
    @MethodSource("notFoundCases")
    fun `test not found resolution cases`(tc: NotFoundCase) {
        val ex = assertThrows<IncludeResolutionException> {
            resolver.resolve(tc.includee, PATH_TO_MAIN)
        }
        assertEquals(tc.expectedConsdieredPaths, ex.consideredFilePaths)
    }

    @Test
    fun `test invalid include search path`() {
        assertThrows<InvalidIncludePathException> {
            IncludeResolver(
                listOf(Paths.get("/this/dir/does/not/exist")),
                FileSystems.getDefault()
            )
        }
    }
}

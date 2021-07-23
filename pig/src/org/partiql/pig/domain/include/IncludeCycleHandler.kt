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

package org.partiql.pig.domain.include

import org.partiql.pig.domain.model.Statement
import org.partiql.pig.domain.parser.TypeUniverseParser
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

/**
 * Prevents cycles in files included with the `include_file` statement from becoming a problem.
 *
 * This is accomplished by keeping track of all files "seen" by the parser and then making any attempt to
 * include a file that was previously seen a no-op.
 *
 * @param mainTypeUniversePath The path to the main type universe that was passed on the command-line. This is the
 * first "seen" file and does not require resolution because the user gave an explicit path to its location.
 *
 * @param resolver For identifying the full path to the file to be included.
 *
 * @see IncludeResolver
 * @see FileSystem
 */
internal class IncludeCycleHandler(
    mainTypeUniversePath: Path,
    private val resolver: IncludeResolver
) {
    private val seenFiles = HashSet<Path>().apply { add(mainTypeUniversePath.toAbsolutePath().normalize()) }

    /**
     * Parses a universe file included with `include_file`.
     *
     * The return value is a [List] of [Statement]s that make up the type universe file.
     *
     * This function becomes a no-op in the event that the [includee] has been seen previously: an
     * empty [List] is is returned instead of the file being parsed again.
     *
     * @param includeePath The file requested to be included.
     * @param includerPath The file in which the includee is to be included.
     */
    fun parseIncludedTypeUniverse(includeePath: String, includerPath: Path): List<Statement> {

        val resolvedIncludeFile = resolver.resolve(resolver.fileSystem.getPath(includeePath), includerPath)

        return if(!seenFiles.contains(resolvedIncludeFile)) {
            seenFiles.add(resolvedIncludeFile)
            Files.newInputStream(resolvedIncludeFile).use {
                val source = InputSource(resolvedIncludeFile, it)
                val parser = TypeUniverseParser(source, this)
                parser.parse()
            }.statements
        } else {
            listOf()
        }
    }
}


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

package org.partiql.pig.domain.parser.include

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages the task of locating a file that was specified with the `include_file` statement.
 *
 * Terminology:
 *
 * - "includer": is a file containing an `include_file` statement. i.e. it is the file doing the including.
 * - "includee": is a file being included by the includer.
 *
 * @param searchDirs A list of directories to search after searching the directory containing the includer, normally
 * specified on the command-line.
 * @param fileSystem The instance of [FileSystem] to be used.  Allowing this to be specified instead of always using
 * the default [FileSystem] allows an in-memory file system to be used during tests. For production uses, use the
 * [FileSystem] returned from [FileSystems.getDefault] here.
 *
 * @throws InvalidIncludePathException if one of [searchDirs] does not exist or is not a directory.
 */
internal class IncludeResolver(
    searchDirs: List<Path>,
    internal val fileSystem: FileSystem
) {

    private val searchDirectories = searchDirs
        .map { it.toAbsolutePath().normalize() }
        .onEach {
            validatePath(it)
            if (!Files.isDirectory(it)) {
                throw InvalidIncludePathException(it.toString())
            }
        }.toTypedArray()

    private fun validatePath(it: Path) {
        require(it.fileSystem === fileSystem) { "Path $it must not belong to a different FileSystem instance." }
    }

    /**
     * Searches for the absolute path of an included file, returning the first file found.
     *
     * The parent directory of the [includerFile] is searched first, followed by each of the source roots in turn. 
     *
     * @return The absolute [Path] of the first file located.
     */
    fun resolve(includeeFile: Path, includerFile: Path): Path {
        val normalizedIncluder = includerFile.normalize().toAbsolutePath()

        // Determine list of all possible search roots
        val includerParentDir = normalizedIncluder.parent

        val searchPaths = listOf(includerParentDir, *searchDirectories).distinct()
        // distinct is needed because duplicate entries can arise in this list, for instance if the
        // includer's parent directory is the same as an include directory. Primarily this is needed to
        // prevent the directory from appearing twice in the error messages we display when we can't
        // find an include file.

        val possibleIncludeeFiles = searchPaths
            .map {
                val appendPath = when {
                    includeeFile.isAbsolute -> includeeFile.toString().substring(1)
                    else -> includeeFile.toString()
                }
                it.resolve(appendPath)
            }


        // The resolved file is the first one that exists.
        return possibleIncludeeFiles.firstOrNull { Files.exists(it) }
            ?: throw IncludeResolutionException(
                inputFilePathString = includeeFile.toString(),
                consideredFilePaths = possibleIncludeeFiles.map { "$it" })
    }
}

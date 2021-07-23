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

package org.partiql.pig.util

import com.google.common.collect.ImmutableList
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseMainTypeUniverse
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path


internal const val FAKE_ROOT_DIR = "/fake-directory"
internal fun makeFakePath(fileName: String) = "$FAKE_ROOT_DIR/$fileName"

/** The name of the "fake" root file used by unit tests. */
internal val FAKE_ROOT_FILE = makeFakePath("root.ion")


/**
 * For unit tests only. Parses the type universe specified in [topUnvierseText].
 *
 * Accomplishes this by using Jimfs to create an in-memory file system, and then
 * writing [topUnvierseText] to [FAKE_ROOT_FILE] within it.
 */
internal fun parseTypeUniverseString(topUnvierseText: String): TypeUniverse {
    val build = Configuration.unix().toBuilder().setWorkingDirectory("/").build()
    val fs: FileSystem = Jimfs.newFileSystem(build)
    Files.createDirectory(fs.getPath(FAKE_ROOT_DIR))
    val rootPath: Path = fs.getPath(FAKE_ROOT_FILE)

    Files.write(rootPath, ImmutableList.of(topUnvierseText), StandardCharsets.UTF_8)

    return parseMainTypeUniverse(rootPath, listOf())
}

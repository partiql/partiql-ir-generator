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

import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.ImportSource
import org.partiql.pig.domain.parser.TypeUniverseParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * The name of the "fake" root file used by unit tests.
 */
internal const val FAKE_ROOT_FILE = "root.ion"

/**
 * For unit tests only. Parses the type universe specified in [topUnvierseText].
 *
 *
 *
 * [includes] is a map keyed by "fake" filename that will be used instead of a real-file system for looking
 * up the content of imported files.  [includes] must not contain any filename by the name of "root.ion", which
 * is the name given to the type universe specified in [topUnvierseText].
 */
fun parseTypeUniverseString(topUnvierseText: String, includes: Map<String, String> = emptyMap()): TypeUniverse {
    assert(!includes.containsKey(FAKE_ROOT_FILE))

    val allIncludes = (mapOf(FAKE_ROOT_FILE to topUnvierseText) + includes).map {
        File(it.key).canonicalPath to it.value
    }.toMap()

    val parser = TypeUniverseParser(FakeImportSource(allIncludes))
    return parser.parseTypeUniverse(FAKE_ROOT_FILE)
}

/** A minimal faux file system backed by a Map<String, String>.  Used only for testing. */
class FakeImportSource(val sources: Map<String, String>) : ImportSource {
    override fun openInputStream(resolvedName: String): InputStream {
        val text: String = sources[resolvedName] ?: throw FileNotFoundException("$resolvedName does not exist")

        return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
    }
}

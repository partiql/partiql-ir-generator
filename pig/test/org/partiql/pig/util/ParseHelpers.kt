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
import org.partiql.pig.domain.parser.InputSource
import org.partiql.pig.domain.parser.Parser
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * For testing purposes, parses the type universe specified in [topUnvierseText].
 *
 * [includes] is a map keyed by "fake" filename that will be used instead of a real-file system for looking
 * up the content of imported files.  [includes] must not contain any filename by the name of "root.ion", which
 * is the name given to the type universe specified in [topUnvierseText].
 */
fun parseTypeUniverseInString(topUnvierseText: String, includes: Map<String, String> = emptyMap()): TypeUniverse {
    assert(!includes.containsKey("root.ion"))
    val allIncludes = mapOf("root.ion" to topUnvierseText) + includes
    val parser = Parser(StringSource(allIncludes))
    return parser.parseTypeUniverse("root.ion")
}

class StringSource(val sources: Map<String, String>) : InputSource {
    override fun openStream(sourceName: String): InputStream {
        val text: String = sources[sourceName] ?: throw FileNotFoundException("$sourceName does not exist")

        return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
    }

    override fun getCanonicalName(sourceName: String): String {
        TODO("not implemented")
    }
}


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

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Provides an abstraction for file-system related functions used when importing files.
 */
internal interface InputSource {
    /**
     * Opens an input stream for the given source name.
     *
     * The [sourceName] is implementation-defined.  In the case of a file system implementaiton it is the path to a
     * file, either relative to the working directory or absolute.
     */
    fun openStream(sourceName: String): InputStream

    /**
     * Returns the "canonical name" of the given source.  In the case of a file system, this converts the relative
     * path to an absolute path.
     */
    fun getCanonicalName(sourceName: String): String
}

internal val FILE_SYSTEM_SOURCE = object : InputSource {
    override fun openStream(qualifiedSource: String) = FileInputStream(qualifiedSource)

    override fun getCanonicalName(sourceName: String): String = File(sourceName).canonicalFile.toString()
}
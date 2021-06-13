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
 *
 * This exists to allow code requiring these functions to be tested with a fake implementation. Many unit tests for
 * example put test type universes in strings within the test itself.  Those tests can't create `FileInputStream`
 * instances or convert relative paths to absolute.  Hence, a "fakeable" interface to these two operations is needed.
 */
internal interface ImportSource {
    /**
     * Opens an input stream for the given source name.
     *
     * The exact meaning of [resolvedSourceName] is implementation-defined.  In the case of a file system
     * implementation it is the path to a file, either relative to the working directory or absolute.
     *
     * The caller must be sure to close the [InputStream].
     */
    fun openInputStream(resolvedSourceName: String): InputStream
}


/** A "real" file system used by production code. */
internal val FILE_IMPORT_SOURCE = object : ImportSource {
    override fun openInputStream(resolvedName: String) = FileInputStream(resolvedName)
}


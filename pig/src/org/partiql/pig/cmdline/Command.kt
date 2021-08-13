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

package org.partiql.pig.cmdline

import java.nio.file.Path

/** Represents command line options specified by the user. */
sealed class Command {

    /** The `--help` command. */
    object ShowHelp : Command()

    /**
     * Returned by [CommandLineParser] when the user has specified invalid command-line arguments
     *
     * - [message]: an error message to be displayed to the user.
     */
    data class InvalidCommandLineArguments(val message: String) : Command()

    /**
     * Contains the details of a *valid* command-line specified by the user.
     *
     * - [typeUniverseFilePath]: the path to the type universe file.
     * - [outputFilePath]: the path to the output file.  (This makes the assumption that there is only one output file.)
     * - [includePaths]: directories to be searched when looking for files included with `include_file`.
     * - [target]: specifies the target language and any other parameters unique to the target language.
     */
    data class Generate(
        val typeUniverseFilePath: Path,
        val outputFilePath: Path,
        val includePaths: List<Path>,
        val target: TargetLanguage
    ) : Command()
}




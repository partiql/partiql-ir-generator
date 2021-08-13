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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths

class CommandLineParserTests {

    @ParameterizedTest
    @MethodSource("parametersForTests")
    fun tests(tc: TestCase) {
        val parser = CommandLineParser()
        val action = parser.parse(tc.args.toTypedArray())

        assertEquals(tc.expected, action)
    }

    companion object {

        class TestCase(val expected: Command, val args: List<String>) {
            constructor(expected: Command, vararg args: String) : this(expected, args.toList())
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForTests(): List<TestCase> {

            return listOf(
                // Help
                TestCase(Command.ShowHelp, "-h"),
                TestCase(Command.ShowHelp, "--help"),

                ////////////////////////////////////////////////////////
                // Missing parameters required for all language targets
                ////////////////////////////////////////////////////////
                // No --universe
                TestCase(
                    Command.InvalidCommandLineArguments("Missing required option(s) [u/universe]"),
                    "--target=kotlin", "--output=output.kt", "--namespace=some.package"),

                // No --target
                TestCase(
                    Command.InvalidCommandLineArguments("Missing required option(s) [t/target]"),
                    "--universe=input.ion", "--output=output.kt", "--namespace=some.package"),

                // No --output argument
                TestCase(
                    Command.InvalidCommandLineArguments("Missing required option(s) [o/output]"),
                    "--universe=input.ion", "--target=kotlin", "--namespace=some.package"),

                ////////////////////////////////////////////////////////
                // Kotlin target
                ////////////////////////////////////////////////////////
                // long parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Kotlin("some.package"), "dir_a", "dir_b"),
                    "--universe=input.ion",
                    "--target=kotlin",
                    "--output=output.any",
                    "--include=dir_a",
                    "--include=dir_b",
                    "--namespace=some.package"),

                // short parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Kotlin("some.package"), "dir_a", "dir_b"),
                    "-u=input.ion",
                    "-t=kotlin",
                    "-o=output.any",
                    "-I=dir_a",
                    "-I=dir_b",
                    "-n=some.package"
                ),

                // no include directories
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Kotlin("some.package")),
                    "-u=input.ion",
                    "-t=kotlin",
                    "-o=output.any",
                    "-n=some.package"
                ),

                // missing the --namespace argument
                TestCase(
                    Command.InvalidCommandLineArguments("The selected language target requires the --namespace argument"),
                    "-u=input.ion", "-t=kotlin", "-o=output.any"),

                ////////////////////////////////////////////////////////
                // Html target
                ////////////////////////////////////////////////////////
                // long parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Html),
                    "--universe=input.ion", "--target=html", "--output=output.any"),

                // short parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Html),
                    "-u=input.ion", "-target=html", "--output=output.any"),

                ////////////////////////////////////////////////////////
                // Custom target
                ////////////////////////////////////////////////////////
                // long parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Custom(Paths.get("template.ftl").toAbsolutePath())),
                    "--universe=input.ion", "--target=custom", "--output=output.any", "--template=template.ftl"),

                // short parameter names
                TestCase(
                    createExpectedGenerateCommand(TargetLanguage.Custom(Paths.get("template.ftl").toAbsolutePath())),
                    "-u=input.ion", "-t=custom", "-o=output.any", "-e=template.ftl"),

                // missing the --template argument
                TestCase(
                    Command.InvalidCommandLineArguments("The selected language target requires the --template argument"),
                    "-u=input.ion", "-t=custom", "-o=output.any")
            )
        }

        private fun createExpectedGenerateCommand(
            target: TargetLanguage,
            vararg additionalIncludePaths: String
        ) = Command.Generate(
            typeUniverseFilePath = Paths.get("input.ion").toAbsolutePath(),
            outputFilePath = Paths.get("output.any").toAbsolutePath(),
            includePaths = listOf(
                Paths.get(".").toAbsolutePath().normalize(),
                *additionalIncludePaths.map { Paths.get(it).toAbsolutePath() }.toTypedArray()
            ),
            target = target
        )
    }
}
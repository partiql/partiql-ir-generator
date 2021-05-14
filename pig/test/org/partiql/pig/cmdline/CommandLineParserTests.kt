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
import java.io.File

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
        fun parametersForTests() = listOf(
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

            // No the --output argument
            TestCase(
                Command.InvalidCommandLineArguments("Missing required option(s) [o/output]"),
                "--universe=input.ion", "--target=kotlin", "--namespace=some.package"),

            ////////////////////////////////////////////////////////
            // Kotlin target
            ////////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate("input.ion", "output.kt", TargetLanguage.Kotlin("some.package")),
                "--universe=input.ion", "--target=kotlin", "--output=output.kt", "--namespace=some.package"),

            // short parameter names
            TestCase(
                Command.Generate("input.ion", "output.kt", TargetLanguage.Kotlin("some.package")),
                "-u=input.ion", "-t=kotlin", "-o=output.kt", "-n=some.package"),

            // missing the --namespace argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --namespace argument"),
                "-u=input.ion", "-t=kotlin", "-o=output.kt"),

            ////////////////////////////////////////////////////////
            // Html target
            ////////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate("input.ion", "output.html", TargetLanguage.Html),
                "--universe=input.ion", "--target=html", "--output=output.html"),

            // short parameter names
            TestCase(
                Command.Generate("input.ion", "output.html", TargetLanguage.Html),
                "-u=input.ion", "-target=html", "--output=output.html"),

            ////////////////////////////////////////////////////////
            // Custom target
            ////////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate("input.ion", "output.txt", TargetLanguage.Custom(File("template.ftl"))),
                "--universe=input.ion", "--target=custom", "--output=output.txt", "--template=template.ftl"),

            // short parameter names
            TestCase(
                Command.Generate("input.ion", "output.txt", TargetLanguage.Custom(File("template.ftl"))),
                "-u=input.ion", "-t=custom", "-o=output.txt", "-e=template.ftl"),

            // missing the --template argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --template argument"),
                "-u=input.ion", "-t=custom", "-o=output.kt")
        )
    }
}
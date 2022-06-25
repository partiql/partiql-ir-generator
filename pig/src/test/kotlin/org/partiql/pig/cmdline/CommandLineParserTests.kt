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

            // //////////////////////////////////////////////////////
            // Missing parameters required for all language targets
            // //////////////////////////////////////////////////////
            // No --universe
            TestCase(
                Command.InvalidCommandLineArguments("Missing required option(s) [u/universe]"),
                "--target=kotlin", "--output-directory=out_dir", "--namespace=some.package"
            ),

            // No --target
            TestCase(
                Command.InvalidCommandLineArguments("Missing required option(s) [t/target]"),
                "--universe=input.ion", "--output-directory=out_dir", "--namespace=some.package"
            ),

            // No --output-file argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --output-file argument"),
                "--universe=input.ion", "--target=custom", "--template=some.template"
            ),

            // No --output-directory argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --output-directory argument"),
                "--universe=input.ion", "--target=kotlin", "--namespace=some.package"
            ),

            // //////////////////////////////////////////////////////
            // Kotlin target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"))),
                "--universe=input.ion", "--target=kotlin", "--output-directory=out_dir", "--namespace=some.package"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"))),
                "-u=input.ion", "-t=kotlin", "-d=out_dir", "-n=some.package"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"), domains = setOf("foo", "bar"))),
                "--universe=input.ion", "--target=kotlin", "--output-directory=out_dir", "--namespace=some.package", "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"), domains = setOf("foo", "bar"))),
                "-u=input.ion", "-t=kotlin", "-d=out_dir", "-n=some.package", "-f=foo,bar"
            ),

            // missing the --namespace argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --namespace argument"),
                "-u=input.ion", "-t=kotlin", "-o=out_dir"
            ),

            // //////////////////////////////////////////////////////
            // Html target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"))),
                "--universe=input.ion", "--target=html", "--output-file=output.html"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"))),
                "-u=input.ion", "-target=html", "--output-file=output.html"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"), domains = setOf("foo", "bar"))),
                "--universe=input.ion", "--target=html", "--output-file=output.html", "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"), domains = setOf("foo", "bar"))),
                "-u=input.ion", "-target=html", "--output-file=output.html", "-f=foo,bar"
            ),

            // //////////////////////////////////////////////////////
            // Ion target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"))),
                "--universe=input.ion", "--target=ion", "--output-file=output.ion"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"))),
                "-u=input.ion", "-target=ion", "--output-file=output.ion"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"), domains = setOf("foo", "bar"))),
                "--universe=input.ion", "--target=ion", "--output-file=output.ion", "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"), domains = setOf("foo", "bar"))),
                "-u=input.ion", "-target=ion", "--output-file=output.ion", "-f=foo,bar"
            ),

            // //////////////////////////////////////////////////////
            // Custom target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"))),
                "--universe=input.ion", "--target=custom", "--output-file=output.txt", "--template=template.ftl"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"))),
                "-u=input.ion", "-t=custom", "-o=output.txt", "-e=template.ftl"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"), domains = setOf("foo", "bar"))),
                "--universe=input.ion", "--target=custom", "--output-file=output.txt", "--template=template.ftl", "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"), domains = setOf("foo", "bar"))),
                "-u=input.ion", "-target=custom", "--output-file=output.txt", "-e=template.ftl", "-f=foo,bar"
            ),

            // missing the --template argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target requires the --template argument"),
                "-u=input.ion", "-t=custom", "-o=output.kt"
            )
        )
    }
}

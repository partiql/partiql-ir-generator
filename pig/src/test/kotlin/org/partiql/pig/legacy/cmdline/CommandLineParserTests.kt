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

package org.partiql.pig.legacy.cmdline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.Pig
import org.partiql.pig.legacy.LegacyCommand
import picocli.CommandLine
import java.io.File

class CommandLineParserTests {

    class CommandLineParser {

        fun parse(args: Array<out String>): Command {
            val cli = CommandLine(Pig()).setCaseInsensitiveEnumValuesAllowed(true)
            cli.execute(*args)
            return LegacyCommand.previous!!
        }
    }

    @ParameterizedTest(name = "TestCase: {index} : {0}")
    @MethodSource("parametersForTests")
    fun tests(tc: TestCase) {
        val parser = CommandLineParser()
        val action = parser.parse(tc.args.toTypedArray())

        assertEquals(tc.expected, action)
    }

    companion object {

        class TestCase(val expected: Command, val args: List<String>) {
            constructor(expected: Command, vararg args: String) : this(expected, args.toList())

            override fun toString() = "pig ${args.joinToString(" ")}"
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForTests() = listOf(
            // // 1. Help
            // TestCase(Command.ShowHelp, "legacy", "--capture", "-h"),
            // // 2. Help
            // TestCase(Command.ShowHelp, "legacy", "--capture", "--help"),
            //
            // // //////////////////////////////////////////////////////
            // // Missing parameters required for all language targets
            // // //////////////////////////////////////////////////////
            // // 3. No --universe
            // TestCase(
            //     Command.InvalidCommandLineArguments("Missing required option(s) [u/universe]"),
            //     "legacy",
            //     "--capture",
            //     "--target=kotlin",
            //     "--output-directory=out_dir",
            //     "--namespace=some.package"
            // ),
            //
            // // 4. No --target
            // TestCase(
            //     Command.InvalidCommandLineArguments("Missing required option(s) [t/target]"),
            //     "legacy",
            //     "--capture",
            //     "--universe=input.ion",
            //     "--output-directory=out_dir",
            //     "--namespace=some.package"
            // ),
            //
            // // 5. No --output-file argument
            // TestCase(
            //     Command.InvalidCommandLineArguments("The selected language target CUSTOM requires the --output-file argument"),
            //     "legacy",
            //     "--capture",
            //     "--universe=input.ion",
            //     "--target=custom",
            //     "--template=some.template"
            // ),
            //
            // // 6. No --output-directory argument
            // TestCase(
            //     Command.InvalidCommandLineArguments("The selected language target KOTLIN requires the --output-directory argument"),
            //     "legacy",
            //     "--capture",
            //     "--universe=input.ion",
            //     "--target=kotlin",
            //     "--namespace=some.package"
            // ),

            // //////////////////////////////////////////////////////
            // Kotlin target
            // //////////////////////////////////////////////////////

            // 7. long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"))),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=kotlin",
                "--output-directory=out_dir",
                "--namespace=some.package"
            ),

            // 8. short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Kotlin("some.package", File("out_dir"))),
                "legacy",
                "--capture",
                "-u=input.ion",
                "-t=kotlin",
                "-d=out_dir",
                "-n=some.package"
            ),

            // 9. accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Kotlin("some.package", File("out_dir"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=kotlin",
                "--output-directory=out_dir",
                "--namespace=some.package",
                "--domains=foo,bar"
            ),

            // 10. accepts -f to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Kotlin("some.package", File("out_dir"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "-u=input.ion",
                "-t=kotlin",
                "-d=out_dir",
                "-n=some.package",
                "-f=foo,bar"
            ),

            // 11. missing the --namespace argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target KOTLIN requires the --namespace argument"),
                "legacy",
                "--capture",
                "-u=input.ion",
                "-t=kotlin",
                "-o=out_dir"
            ),

            // //////////////////////////////////////////////////////
            // Html target
            // //////////////////////////////////////////////////////

            // 12. long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"))),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=html",
                "--output-file=output.html"
            ),

            // 13. short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Html(File("output.html"))),
                "legacy",
                "--capture",
                "-u=input.ion",
                "--target=html",
                "--output-file=output.html"
            ),

            // 14. accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Html(File("output.html"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=html",
                "--output-file=output.html",
                "--domains=foo,bar"
            ),

            // 15. accepts -f to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Html(File("output.html"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "-u=input.ion",
                "--target=html",
                "--output-file=output.html",
                "-f=foo,bar"
            ),

            // //////////////////////////////////////////////////////
            // Ion target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"))),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=ion",
                "--output-file=output.ion"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Ion(File("output.ion"))),
                "legacy",
                "--capture",
                "-u=input.ion",
                "--target=ion",
                "--output-file=output.ion"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Ion(File("output.ion"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=ion",
                "--output-file=output.ion",
                "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Ion(File("output.ion"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "-u=input.ion",
                "--target=ion",
                "--output-file=output.ion",
                "-f=foo,bar"
            ),

            // //////////////////////////////////////////////////////
            // Custom target
            // //////////////////////////////////////////////////////
            // long parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"))),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=custom",
                "--output-file=output.txt",
                "--template=template.ftl"
            ),

            // short parameter names
            TestCase(
                Command.Generate(File("input.ion"), TargetLanguage.Custom(File("template.ftl"), File("output.txt"))),
                "legacy",
                "--capture",
                "-u=input.ion",
                "-t=custom",
                "-o=output.txt",
                "-e=template.ftl"
            ),

            // accepts --domains to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Custom(File("template.ftl"), File("output.txt"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "--universe=input.ion",
                "--target=custom",
                "--output-file=output.txt",
                "--template=template.ftl",
                "--domains=foo,bar"
            ),

            // accepts -f to filter for specific domains
            TestCase(
                Command.Generate(
                    File("input.ion"),
                    TargetLanguage.Custom(File("template.ftl"), File("output.txt"), domains = setOf("foo", "bar"))
                ),
                "legacy",
                "--capture",
                "-u=input.ion",
                "--target=custom",
                "--output-file=output.txt",
                "-e=template.ftl",
                "-f=foo,bar"
            ),

            // missing the --template argument
            TestCase(
                Command.InvalidCommandLineArguments("The selected language target CUSTOM requires the --template argument"),
                "legacy",
                "--capture",
                "-u=input.ion",
                "-t=custom",
                "-o=output.kt"
            )
        )
    }
}

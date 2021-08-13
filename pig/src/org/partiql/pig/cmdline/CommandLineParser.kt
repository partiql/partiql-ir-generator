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

import joptsimple.*
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths


class CommandLineParser {
    private enum class LanguageTargetType(
        val requireNamespace: Boolean = false,
        val requireTemplate: Boolean = false
    ) {
        KOTLIN(requireNamespace = true),
        CUSTOM(requireTemplate = true),
        HTML
    }

    private object LanguageTargetTypeValueConverter : ValueConverter<LanguageTargetType> {
        private val lookup = LanguageTargetType.values().associateBy { it.name.toLowerCase() }

        override fun convert(value: String?): LanguageTargetType {
            if(value == null) throw ValueConversionException("Value was null")
            return lookup[value] ?: throw ValueConversionException("Invalid language target type: ${value}")
        }

        override fun valueType(): Class<out LanguageTargetType> {
            return LanguageTargetType::class.java
        }

        override fun valuePattern(): String {
            return LanguageTargetType.values().map { it.name.toLowerCase() }.joinToString("|")
        }
    }

    private object PathValueConverter : ValueConverter<Path> {
        override fun convert(value: String?): Path = Paths.get(value).toAbsolutePath().normalize()
        override fun valueType(): Class<out Path> = Path::class.java
        override fun valuePattern(): String? = null
    }

    private val formatter = object : BuiltinHelpFormatter(120, 2) {
        override fun format(options: MutableMap<String, out OptionDescriptor>?): String {
            return """PartiQL I.R. Generator
                |
                |${super.format(options)}
                |Notes:
                |
                |  --target=kotlin requires --namespace=<ns>
                |  --target=custom requires --template=<path-to-template>
                |  All paths specified in these command-line options are relative to the current working
                |  directory by default.
                | 
                |Examples:
                |
                |  pig --target=kotlin --universe=universe.ion --output=example.kt --namespace=org.example.domain
                |  pig --target=custom --universe=universe.ion --output=example.txt --template=template.ftl
                |     
        """.trimMargin()
        }
    }
    private val optParser = OptionParser().apply {
        formatHelpWith(formatter)
    }


    private val helpOpt = optParser.acceptsAll(listOf("help", "h", "?"), "prints this help")
        .forHelp()

    private val universeOpt = optParser.acceptsAll(listOf("universe", "u"), "Type universe input file")
        .withRequiredArg()
        .withValuesConvertedBy(PathValueConverter)
        .required()

    private val includeSearchRootOpt = optParser.acceptsAll(listOf("include", "I"), "Include search path")
        .withRequiredArg()
        .withValuesConvertedBy(PathValueConverter)
        .describedAs("Search path for files included with include_file. May be specified multiple times.")

    private val outputOpt = optParser.acceptsAll(listOf("output", "o"), "Generated output file")
        .withRequiredArg()
        .withValuesConvertedBy(PathValueConverter)
        .required()

    private val targetTypeOpt = optParser.acceptsAll(listOf("target", "t"), "Target language")
        .withRequiredArg()
        .withValuesConvertedBy(LanguageTargetTypeValueConverter)
        .required()

    private val namespaceOpt = optParser.acceptsAll(listOf("namespace", "n"), "Namespace for generated code")
        .withRequiredArg()
        .ofType(String::class.java)

    private val templateOpt = optParser.acceptsAll(listOf("template", "e"), "Path to an Apache FreeMarker template")
        .withOptionalArg()
        .withValuesConvertedBy(PathValueConverter)


    /**
     * Prints help to the specified [PrintStream].
     */
    fun printHelp(pw: PrintStream) {
        optParser.printHelpOn(pw)
    }

    /**
     * Parses the command-line arguments.
     *
     * Returns `null` if the command-line arguments were invalid or
     */
    fun parse(args: Array<String>): Command {
        return try {
            val optSet = optParser.parse(*args)

            when {
                optSet.has(helpOpt) -> Command.ShowHelp
                else -> {
                    // !! is fine in this case since we define these options as .required() above.
                    val typeUniverseFile: Path = optSet.valueOf(universeOpt)!!
                    val targetType = optSet.valueOf(targetTypeOpt)!!
                    val outputFile: Path = optSet.valueOf(outputOpt)!!

                    // Always add the parent of the file containing the main type universe as an include root.
                    val includeSearchRoots = listOf(
                        typeUniverseFile.parent,
                        *optSet.valuesOf(includeSearchRootOpt)!!.toTypedArray()
                    )

                    if (targetType.requireNamespace) {
                        if (!optSet.has(namespaceOpt)) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target requires the --namespace argument")
                        }
                    } else if(optSet.has(namespaceOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --namespace argument")
                    }

                    if(targetType.requireTemplate) {
                        if(!optSet.has(templateOpt)) {
                            return Command.InvalidCommandLineArguments("The selected language target requires the --template argument")
                        }
                    } else if(optSet.has(templateOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --template argument")
                    }

                    val target = when(targetType) {
                        LanguageTargetType.HTML -> TargetLanguage.Html
                        LanguageTargetType.KOTLIN -> TargetLanguage.Kotlin(optSet.valueOf(namespaceOpt))
                        LanguageTargetType.CUSTOM -> TargetLanguage.Custom(optSet.valueOf(templateOpt))
                    }

                    Command.Generate(typeUniverseFile, outputFile, includeSearchRoots, target)
                }
            }
        } catch(ex: OptionException) {
            Command.InvalidCommandLineArguments(ex.message!!)
        }
    }
}

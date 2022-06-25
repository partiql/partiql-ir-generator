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

import joptsimple.BuiltinHelpFormatter
import joptsimple.OptionDescriptor
import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.ValueConversionException
import joptsimple.ValueConverter
import java.io.File
import java.io.PrintStream

class CommandLineParser {
    private enum class LanguageTargetType(
        val requireNamespace: Boolean,
        val requireTemplateFile: Boolean,
        val requireOutputFile: Boolean,
        val requireOutputDirectory: Boolean
    ) {
        KOTLIN(requireNamespace = true, requireTemplateFile = false, requireOutputFile = false, requireOutputDirectory = true),
        CUSTOM(requireNamespace = false, requireTemplateFile = true, requireOutputFile = true, requireOutputDirectory = false),
        HTML(requireNamespace = false, requireTemplateFile = false, requireOutputFile = true, requireOutputDirectory = false),
        ION(requireNamespace = false, requireTemplateFile = false, requireOutputFile = true, requireOutputDirectory = false)
    }

    private object LanguageTargetTypeValueConverter : ValueConverter<LanguageTargetType> {
        private val lookup = LanguageTargetType.values().associateBy { it.name.toLowerCase() }

        override fun convert(value: String?): LanguageTargetType {
            if (value == null) throw ValueConversionException("Value was null")
            return lookup[value] ?: throw ValueConversionException("Invalid language target type: $value")
        }

        override fun valueType(): Class<out LanguageTargetType> {
            return LanguageTargetType::class.java
        }

        override fun valuePattern(): String {
            return LanguageTargetType.values().map { it.name.toLowerCase() }.joinToString("|")
        }
    }

    private object DomainFilterValueConverter : ValueConverter<Set<String>> {
        override fun convert(value: String?): Set<String> {
            if (value.isNullOrBlank()) throw ValueConversionException("Value was empty")
            return value.split(',').map(String::trim).toSet()
        }

        override fun valueType(): Class<out Set<String>>? = null

        override fun valuePattern(): String {
            return "<domain 1>,<domain 2>,..."
        }
    }


    private val formatter = object : BuiltinHelpFormatter(120, 2) {
        override fun format(options: MutableMap<String, out OptionDescriptor>?): String {
            return """PartiQL I.R. Generator
                |
                |${super.format(options)}
                |Each target requires certain arguments:
                |
                |   --target=kotlin requires --namespace=<ns> and --output-directory=<out-dir>
                |   --target=custom requires --template=<path-to-template> and --output-file=<generated-file>
                |   --target=html   requires --output-file=<output-html-file>
                |   --target=ion    requires --output-file=<output-ion-file>
                |
                |Notes:
                |
                |   If -d or --output-directory is specified and the directory does not exist, it will be created. 
                | 
                |Examples:
                |
                |  pig --target=kotlin \   
                |      --universe=universe.ion \ 
                |      --output-directory=generated-src \
                |      --namespace=org.example.domain
                |  
                |  pig --target=custom \
                |      --universe=universe.ion \ 
                |      --output-file=example.txt \
                |      --template=template.ftl 
                |     
                |  pig --target=ion \
                |      --universe=universe.ion \ 
                |      --output-file=example.ion
                |     
        ""${'"'}.trimMargin()
        """.trimMargin()
        }
    }
    private val optParser = OptionParser().also { it.formatHelpWith(formatter) }

    private val helpOpt = optParser.acceptsAll(listOf("help", "h", "?"), "prints this help")
        .forHelp()

    private val universeOpt = optParser.acceptsAll(listOf("universe", "u"), "Type universe input file")
        .withRequiredArg()
        .ofType(File::class.java)
        .required()

    private val targetTypeOpt = optParser.acceptsAll(listOf("target", "t"), "Target language")
        .withRequiredArg()
        .withValuesConvertedBy(LanguageTargetTypeValueConverter)
        .required()

    private val outputFileOpt = optParser.acceptsAll(listOf("output-file", "o"), "Generated output file (for targets that output a single file)")
        .withRequiredArg()
        .ofType(File::class.java)

    private val outputDirectoryOpt = optParser.acceptsAll(listOf("output-directory", "d"), "Generated output directory (for targets that output multiple files)")
        .withRequiredArg()
        .ofType(File::class.java)

    private val namespaceOpt = optParser.acceptsAll(listOf("namespace", "n"), "Namespace for generated code")
        .withOptionalArg()
        .ofType(String::class.java)

    private val templateOpt = optParser.acceptsAll(listOf("template", "e"), "Path to an Apache FreeMarker template")
        .withOptionalArg()
        .ofType(File::class.java)

    private val domainsOpt = optParser.acceptsAll(listOf("domains", "f"), "List of domains to generate (comma separated)")
        .withOptionalArg()
        .withValuesConvertedBy(DomainFilterValueConverter)

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
                    val typeUniverseFile: File = optSet.valueOf(universeOpt)!!
                    val targetType = optSet.valueOf(targetTypeOpt)!!

                    // --namespace
                    if (targetType.requireNamespace) {
                        if (!optSet.has(namespaceOpt)) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target requires the --namespace argument"
                            )
                        }
                    } else if (optSet.has(namespaceOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --namespace argument"
                        )
                    }

                    // --output-file
                    if (targetType.requireOutputFile) {
                        if (!optSet.has(outputFileOpt)) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target requires the --output-file argument"
                            )
                        }
                    } else if (optSet.has(outputFileOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --output-file argument"
                        )
                    }

                    // --output-directory
                    if (targetType.requireOutputDirectory) {
                        if (!optSet.has(outputDirectoryOpt)) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target requires the --output-directory argument"
                            )
                        }
                    } else if (optSet.has(outputDirectoryOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --output-directory argument"
                        )
                    }

                    // --template
                    if (targetType.requireTemplateFile) {
                        if (!optSet.has(templateOpt)) {
                            return Command.InvalidCommandLineArguments("The selected language target requires the --template argument")
                        }
                    } else if (optSet.has(templateOpt)) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target does not allow the --template argument"
                        )
                    }

                    val domains = optSet.valueOf(domainsOpt)

                    val target = when (targetType) {
                        LanguageTargetType.HTML -> TargetLanguage.Html(
                            outputFile = optSet.valueOf(outputFileOpt) as File,
                            domains = domains
                        )
                        LanguageTargetType.KOTLIN -> TargetLanguage.Kotlin(
                            namespace = optSet.valueOf(namespaceOpt) as String,
                            outputDirectory = optSet.valueOf(outputDirectoryOpt) as File,
                            domains = domains
                        )
                        LanguageTargetType.CUSTOM -> TargetLanguage.Custom(
                            templateFile = optSet.valueOf(templateOpt),
                            outputFile = optSet.valueOf(outputFileOpt) as File,
                            domains = domains
                        )
                        LanguageTargetType.ION -> TargetLanguage.Ion(
                            outputFile = optSet.valueOf(outputFileOpt) as File,
                            domains = domains
                        )
                    }

                    Command.Generate(typeUniverseFile, target)
                }
            }
        } catch (ex: OptionException) {
            Command.InvalidCommandLineArguments(ex.message!!)
        }
    }
}

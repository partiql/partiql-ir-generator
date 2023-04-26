package org.partiql.pig.legacy

import com.amazon.ion.system.IonReaderBuilder
import org.partiql.pig.legacy.cmdline.Command
import org.partiql.pig.legacy.cmdline.TargetLanguage
import org.partiql.pig.legacy.errors.PigException
import org.partiql.pig.legacy.generator.custom.applyCustomTemplate
import org.partiql.pig.legacy.generator.html.applyHtmlTemplate
import org.partiql.pig.legacy.generator.ion.generateIon
import org.partiql.pig.legacy.generator.kotlin.convertToKTypeUniverse
import org.partiql.pig.legacy.generator.kotlin.generateKotlinCode
import org.partiql.pig.legacy.model.TypeUniverse
import org.partiql.pig.legacy.parser.parseTypeUniverse
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.io.FileInputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.util.Optional
import java.util.Properties
import java.util.concurrent.Callable

/**
 * PIG 0.x command line.
 *
 * Optionals are not ideal, but are necessary coming from jopt since subcommands weren't used.
 */
@CommandLine.Command(
    name = "legacy",
    description = ["PartiQL IR Generator 0.x"]
)
class LegacyCommand : Callable<Int> {

    private val properties = Properties()

    init {
        properties.load(this.javaClass.getResourceAsStream("/pig.properties"))
    }

    companion object {

        // Previous command hack for backwards compatibility testing.
        // Picocli parses a command for us, but we want to recreate the old jopt style here.
        // Could be a list, but `pig` is only ever invoked once
        internal var previous: Command? = null
    }

    @Option(
        names = ["--capture"],
        description = ["Parse arguments to previous command, but do not execute"]
    )
    var capture: Boolean = false

    @Option(
        names = ["--help", "-h", "-?"],
        description = ["Prints current version"]
    )
    var help: Boolean = false

    @Option(
        names = ["--version", "-v"],
        description = ["Prints current version"]
    )
    var version: Boolean = false

    @Option(
        names = ["--universe", "-u"],
        description = ["Type universe input file"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var universe: Optional<File>

    @Option(
        names = ["--target", "-t"],
        description = ["Type universe input file"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var target: Optional<LanguageTargetType>

    @Option(
        names = ["--output-file", "-o"],
        description = ["Generated source output file"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var outputFile: Optional<File>

    @Option(
        names = ["--output-directory", "-d"],
        description = ["Generated source output directory"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var outputDirectory: Optional<File>

    @Option(
        names = ["--namespace", "-n"],
        description = ["Namespace for generated code"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var namespace: Optional<String>

    @Option(
        names = ["--template", "-e"],
        description = ["Path to an Apache FreeMarker template"],
        defaultValue = Option.NULL_VALUE
    )
    lateinit var template: Optional<File>

    @Option(
        names = ["--domains", "-f"],
        split = ",",
        description = ["List of domains to generate (comma separated)"]
    )
    var domains: List<String> = emptyList()

    override fun call(): Int {
        val command = parse()
        // short-circuit on capture
        if (capture) {
            previous = command
            return 0
        }
        // execute, legacy main.kt
        when (command) {
            is Command.ShowHelp -> printHelp(System.out)
            is Command.ShowVersion -> printVersion(System.out)
            is Command.InvalidCommandLineArguments -> {
                System.err.println(command.message)
                return -1
            }
            is Command.Generate -> {
                try {
                    generateCode(command)
                } catch (e: PigException) {
                    System.err.println("pig: ${e.error.location}: ${e.error.context.message}\n${e.stackTrace}")
                    return -1
                }
            }
        }
        return 0
    }

    /**
     * Prints help to the specified [PrintStream].
     */
    private fun printHelp(out: PrintStream) {
        val help = CommandLine.Help(this)
        out.println(help)
        out.println(
            """|Each target requires certain arguments:
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
            """.trimMargin()
        )
    }

    /**
     * Prints version specified in the package root build.gradle
     */
    private fun printVersion(out: PrintStream) {
        out.println("PartiQL I.R. Generator Version ${properties.getProperty("version")}")
    }

    private fun parse(): Command {
        return try {
            when {
                help -> Command.ShowHelp
                version -> Command.ShowVersion
                else -> {
                    if (!universe.isPresent) {
                        return Command.InvalidCommandLineArguments("Missing required option(s) [u/universe]")
                    }
                    val typeUniverseFile: File = universe.get()

                    if (!target.isPresent) {
                        return Command.InvalidCommandLineArguments("Missing required option(s) [t/target]")
                    }
                    val targetType = target.get()

                    // --namespace
                    if (targetType.requireNamespace) {
                        if (!namespace.isPresent) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target $targetType requires the --namespace argument"
                            )
                        }
                    } else if (namespace.isPresent) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target $targetType does not allow the --namespace argument"
                        )
                    }

                    // --output-file
                    if (targetType.requireOutputFile) {
                        if (!outputFile.isPresent) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target $targetType requires the --output-file argument"
                            )
                        }
                    } else if (outputFile.isPresent) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target $targetType does not allow the --output-file argument"
                        )
                    }

                    // --output-directory
                    if (targetType.requireOutputDirectory) {
                        if (!outputDirectory.isPresent) {
                            return Command.InvalidCommandLineArguments(
                                "The selected language target $targetType requires the --output-directory argument"
                            )
                        }
                    } else if (outputDirectory.isPresent) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target $targetType does not allow the --output-directory argument"
                        )
                    }

                    // --template
                    if (targetType.requireTemplateFile) {
                        if (!template.isPresent) {
                            return Command.InvalidCommandLineArguments("The selected language target $targetType requires the --template argument")
                        }
                    } else if (template.isPresent) {
                        return Command.InvalidCommandLineArguments(
                            "The selected language target $targetType does not allow the --template argument"
                        )
                    }

                    val domainSet: Set<String>? = when (domains.isNotEmpty()) {
                        true -> domains.toSet()
                        false -> null
                    }

                    val target = when (targetType) {
                        LanguageTargetType.HTML -> TargetLanguage.Html(
                            outputFile = outputFile.get(),
                            domains = domainSet
                        )
                        LanguageTargetType.KOTLIN -> TargetLanguage.Kotlin(
                            namespace = namespace.get(),
                            outputDirectory = outputDirectory.get(),
                            domains = domainSet
                        )
                        LanguageTargetType.CUSTOM -> TargetLanguage.Custom(
                            templateFile = template.get(),
                            outputFile = outputFile.get(),
                            domains = domainSet
                        )
                        LanguageTargetType.ION -> TargetLanguage.Ion(
                            outputFile = outputFile.get(),
                            domains = domainSet
                        )
                    }

                    Command.Generate(typeUniverseFile, target)
                }
            }
        } catch (ex: NoSuchElementException) {
            return Command.InvalidCommandLineArguments(ex.message!!)
        }
    }

    enum class LanguageTargetType(
        val requireNamespace: Boolean,
        val requireTemplateFile: Boolean,
        val requireOutputFile: Boolean,
        val requireOutputDirectory: Boolean
    ) {
        KOTLIN(
            requireNamespace = true,
            requireTemplateFile = false,
            requireOutputFile = false,
            requireOutputDirectory = true
        ),
        CUSTOM(
            requireNamespace = false,
            requireTemplateFile = true,
            requireOutputFile = true,
            requireOutputDirectory = false
        ),
        HTML(
            requireNamespace = false,
            requireTemplateFile = false,
            requireOutputFile = true,
            requireOutputDirectory = false
        ),
        ION(
            requireNamespace = false,
            requireTemplateFile = false,
            requireOutputFile = true,
            requireOutputDirectory = false
        )
    }
}

private fun progress(msg: String) = println("pig: $msg")

/**
 * Gradle projects such as the PartiQL reference implementation take a dependency on this project
 * in their `buildSrc` directory and can then use this entry point to generate code direclty without
 * having to `exec` pig as a separate process.
 */
fun generateCode(command: Command.Generate) {
    progress("universe file: ${command.typeUniverseFile}")

    progress("parsing the universe...")
    val typeUniverse: TypeUniverse = FileInputStream(command.typeUniverseFile).use { inputStream ->
        IonReaderBuilder.standard().build(inputStream).use { ionReader -> parseTypeUniverse(ionReader) }
    }

    progress("permuting domains...")

    val computedDomains = typeUniverse.computeTypeDomains()
    val filteredDomains =
        command.target.domains?.let { computedDomains.filter { domain -> domain.tag in it } } ?: computedDomains

    when (command.target) {
        is TargetLanguage.Kotlin -> {
            progress("applying Kotlin pre-processing")
            val kotlinTypeUniverse = typeUniverse.convertToKTypeUniverse(computedDomains, filteredDomains, command.target.domains)
            val dir = command.target.outputDirectory
            if (dir.exists()) {
                if (!dir.isDirectory) {
                    error("The path specified as the output directory exists but is not a directory.")
                }
            } else {
                dir.mkdirs()
            }
            progress("applying the Kotlin template once for each domain...")
            generateKotlinCode(command.target.namespace, kotlinTypeUniverse, command.target.outputDirectory)
        }
        is TargetLanguage.Custom -> {
            progress("output file  : ${command.target.outputFile}")
            progress("applying ${command.target.templateFile}")

            PrintWriter(command.target.outputFile).use { printWriter ->
                applyCustomTemplate(command.target.templateFile, filteredDomains, printWriter)
            }
        }
        is TargetLanguage.Html -> {
            progress("output file  : ${command.target.outputFile}")
            progress("applying the HTML template")

            PrintWriter(command.target.outputFile).use { printWriter ->
                applyHtmlTemplate(filteredDomains, printWriter)
            }
        }
        is TargetLanguage.Ion -> {
            progress("output file  : ${command.target.outputFile}")
            progress("applying the Ion template")

            PrintWriter(command.target.outputFile).use { printWriter ->
                generateIon(filteredDomains, printWriter)
            }
        }
    }

    progress("universe generation complete!")
}

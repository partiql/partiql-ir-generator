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

package org.partiql.pig

import com.amazon.ion.system.IonReaderBuilder
import org.partiql.pig.cmdline.Command
import org.partiql.pig.cmdline.CommandLineParser
import org.partiql.pig.cmdline.TargetLanguage
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseTypeUniverse
import org.partiql.pig.errors.PigException
import org.partiql.pig.generator.custom.applyCustomTemplate
import org.partiql.pig.generator.html.applyHtmlTemplate
import org.partiql.pig.generator.kotlin.convertToKTypeUniverse
import org.partiql.pig.generator.kotlin.generateKotlinCode
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import kotlin.system.exitProcess

fun progress(msg: String) =
    println("pig: $msg")

/**
 * Entry point for when pig is being invoked from the command-line.
 */
fun main(args: Array<String>) {
    val cmdParser = CommandLineParser()

    when (val command = cmdParser.parse(args)) {
        is Command.ShowHelp -> cmdParser.printHelp(System.out)
        is Command.ShowVersion -> cmdParser.printVersion(System.out)
        is Command.InvalidCommandLineArguments -> {
            System.err.println(command.message)
            exitProcess(-1)
        }
        is Command.Generate -> {
            try {
                generateCode(command)
            } catch (e: PigException) {
                System.err.println("pig: ${e.error.location}: ${e.error.context.message}\n${e.stackTrace}")
                exitProcess(-1)
            }
        }
    }
}

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

    when (command.target) {
        is TargetLanguage.Kotlin -> {
            progress("applying Kotlin pre-processing")
            val kotlinTypeUniverse = typeUniverse.convertToKTypeUniverse()
            prepareOutputDirectory(command.target.outputDirectory)
            progress("applying the Kotlin template once for each domain...")

            generateKotlinCode(command.target.namespace, kotlinTypeUniverse, command.target.outputDirectory)
        }
        is TargetLanguage.Custom -> {
            progress("output file  : ${command.target.outputFile}")
            progress("applying ${command.target.templateFile}")

            PrintWriter(command.target.outputFile).use { printWriter ->
                applyCustomTemplate(command.target.templateFile, typeUniverse.computeTypeDomains(), printWriter)
            }
        }
        is TargetLanguage.Html -> {
            progress("output file  : ${command.target.outputFile}")
            progress("applying the HTML template")
            PrintWriter(command.target.outputFile).use { printWriter ->
                applyHtmlTemplate(typeUniverse.computeTypeDomains(), printWriter)
            }
        }
    }

    progress("universe generation complete!")
}

private fun prepareOutputDirectory(dir: File) {
    if (dir.exists()) {
        if (!dir.isDirectory) {
            error("The path specified as the output directory exists but is not a directory.")
        }
    } else {
        dir.mkdirs()
    }
}

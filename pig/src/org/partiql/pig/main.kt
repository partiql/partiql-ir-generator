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
import org.partiql.pig.errors.PigException
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseTypeUniverseFile
import org.partiql.pig.generator.custom.applyCustomTemplate
import org.partiql.pig.generator.html.applyHtmlTemplate
import org.partiql.pig.generator.kotlin.applyKotlinTemplate
import org.partiql.pig.generator.kotlin.convertToKTypeUniverse
import java.io.FileInputStream
import java.io.PrintWriter
import kotlin.system.exitProcess

 fun progress(msg: String) =
    println("pig: ${msg}")

/**
 * Entry point for when pig is being invoked from the command-line.
 */
fun main(args: Array<String>) {
    val cmdParser = CommandLineParser()

    when(val command = cmdParser.parse(args)) {
        is Command.ShowHelp -> cmdParser.printHelp(System.out)
        is Command.InvalidCommandLineArguments -> {
            System.err.println(command.message)
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
    progress("output file  : ${command.outputFile}")

    progress("parsing the universe...")
    val typeUniverse: TypeUniverse = parseTypeUniverseFile(command.typeUniverseFile)

    progress("permuting domains...")

    PrintWriter(command.outputFile).use { printWriter ->
        when (command.target) {
            is TargetLanguage.Kotlin -> {
                progress("applying Kotlin pre-processing")
                val kotlinTypeUniverse = typeUniverse.convertToKTypeUniverse()

                progress("applying the Kotlin template...")
                applyKotlinTemplate(command.target.namespace, kotlinTypeUniverse, printWriter)
            }
            is TargetLanguage.Custom -> {
                progress("applying ${command.target.templateFile}")
                applyCustomTemplate(command.target.templateFile, typeUniverse.computeTypeDomains(), printWriter)
            }
            is TargetLanguage.Html -> {
                progress("applying the HTML template")
                applyHtmlTemplate(typeUniverse.computeTypeDomains(), printWriter)
            }
        }
    }

    progress("universe generation complete!")
}


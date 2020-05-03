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
import org.partiql.pig.domain.PigException
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseTypeUniverse
import org.partiql.pig.generator.custom.applyCustomTemplate
import org.partiql.pig.generator.html.applyHtmlTemplate
import org.partiql.pig.generator.kotlin.KTypeUniverse
import org.partiql.pig.generator.kotlin.applyKotlinTemplate
import org.partiql.pig.generator.kotlin.toKTypeDomain
import java.io.FileInputStream
import java.io.PrintWriter
import kotlin.system.exitProcess

 fun progress(msg: String) =
    println("pig: ${msg}")

fun main(args: Array<String>) {
    val cmdParser = CommandLineParser()

    when(val command = cmdParser.parse(args)) {
        is Command.ShowHelp -> cmdParser.printHelp(System.out)
        is Command.InvalidCommandLineArguments -> {
            System.err.println(command.message)
        }
        is Command.Generate -> {
            generateCode(command)
        }
    }
}

fun generateCode(command: Command.Generate) {
    progress("universe file: ${command.typeUniverseFile}")
    progress("output file  : ${command.outputFile}")

    val allTypeDomains: List<TypeDomain> = try {
        progress("parsing the universe...")
        val typeUniverse: TypeUniverse = FileInputStream(command.typeUniverseFile).use { inputStream ->
            IonReaderBuilder.standard().build(inputStream).use { ionReader -> parseTypeUniverse(ionReader) }
        }

        progress("permuting domains...")
        typeUniverse.computeTypeDomains()
    } catch (e: PigException) {
        System.err.println("pig: ${e.error.location}: ${e.error.context.message}\n${e.stackTrace}")
        exitProcess(-1)
    }

    PrintWriter(command.outputFile).use { printWriter ->
        when (command.target) {
            is TargetLanguage.Kotlin -> {
                progress("applying Kotlin pre-processing")
                val kotlinTypeUniverse = KTypeUniverse(allTypeDomains.map { it.toKTypeDomain() })

                progress("applying the Kotlin template...")
                applyKotlinTemplate(command.target.namespace, kotlinTypeUniverse, printWriter)
            }
            is TargetLanguage.Custom -> {
                progress("applying ${command.target.templateFile}")
                applyCustomTemplate(command.target.templateFile, allTypeDomains, printWriter)
            }
            is TargetLanguage.Html -> {
                progress("applying the HTML template")
                applyHtmlTemplate(allTypeDomains, printWriter)
            }
        }
    }

    progress("universe generation complete!")
}



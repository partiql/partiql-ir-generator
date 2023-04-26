/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.pig

import org.partiql.pig.generator.target.kotlin.KotlinCommand
import org.partiql.pig.legacy.LegacyCommand
import picocli.CommandLine
import java.io.IOException
import java.util.Properties
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = CommandLine(Pig()).setCaseInsensitiveEnumValuesAllowed(true)
    exitProcess(command.execute(*args))
}

@CommandLine.Command(
    name = "pig",
    subcommands = [Generate::class, LegacyCommand::class]
)
class Pig : Runnable {

    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var help = false

    @CommandLine.Option(names = ["-v", "--version"], description = ["Prints current version"])
    var version = false

    override fun run() {
        if (version) {
            val properties = Properties()
            val version = try {
                properties.load(this.javaClass.getResourceAsStream("/pig.properties"))
                properties.getProperty("version")
            } catch (ex: IOException) {
                "?"
            }
            println(version)
        }
    }
}

@CommandLine.Command(
    name = "generate",
    subcommands = [KotlinCommand::class],
    description = ["PartiQL IR Generator 1.x"]
)
class Generate : Runnable {

    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var help = false

    override fun run() {}
}

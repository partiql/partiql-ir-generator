package org.partiql.pig.generator.target.kotlin

import org.partiql.pig.parser.PigParser
import picocli.CommandLine
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "kotlin",
    description = ["Generates Kotlin sources from type universe definitions"]
)
class KotlinCommand : Callable<Int> {

    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var help = false

    @CommandLine.Parameters(
        index = "0",
        description = ["Type definition file"]
    )
    lateinit var file: File

    @CommandLine.Option(
        names = ["-p", "--package"],
        description = ["Package root"]
    )
    lateinit var packageRoot: String

    @CommandLine.Option(
        names = ["-u", "--universe"],
        description = ["Universe identifier"]
    )
    lateinit var id: String

    @CommandLine.Option(
        names = ["-o", "--out"],
        description = ["Generated source output directory"]
    )
    lateinit var out: Path

    @CommandLine.Option(
        names = ["--poems"],
        split = ",",
        description = ["Poem templates to apply"]
    )
    var poems: List<String> = emptyList()

    @CommandLine.Option(
        names = ["-m", "--modifier"],
        description = ["Generated node class modifier. Options \${COMPLETION-CANDIDATES}"],
        defaultValue = "DATA"
    )
    lateinit var modifier: KotlinNodeOptions.Modifier

    override fun call(): Int {
        val input = BufferedReader(FileInputStream(file).reader()).readText()
        val parser = PigParser.default()
        val universe = parser.parse(id, input)
        val options = KotlinOptions(
            packageRoot = packageRoot,
            poems = poems,
            node = KotlinNodeOptions(
                modifier = modifier
            )
        )
        val generator = KotlinGenerator(options)
        val result = generator.generate(universe)
        // Write all generated files
        result.write {
            val p = it.packageName.replace(".", "/")
            val dir = out.resolve(p).toAbsolutePath()
            Files.createDirectories(dir)
            val file = Files.newBufferedWriter(dir.resolve("${it.name}.kt"))
            it.writeTo(file)
            file.close()
        }
        return 0
    }
}
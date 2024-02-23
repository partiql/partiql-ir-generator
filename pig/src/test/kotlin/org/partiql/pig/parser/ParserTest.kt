package org.partiql.pig.parser

import org.junit.jupiter.api.Test
import org.partiql.pig.model.Document

class ParserTest {

    @Test
    fun load() {
        val input = """
            (product hello name::string)
            
            (sum foo
                (product a foo::int32 bar::hello)
                (product b foo::int32)
            )
            
            (sum bar)
            
            (fixed uuid 16)
            (unit goodbye)
            
            (product parent
                x::bool
                y::bool
                (product child_a
                    z::bool
                )
                (product child_b
                    z::bool
                )
            )
        """.trimIndent()
        val document = Document.load("test", input)
        println(document)
    }
}

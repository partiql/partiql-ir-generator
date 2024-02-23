package org.partiql.pig.model

import org.partiql.pig.parser.Parser
import java.nio.file.Path

/**
 * Top-level model of a grammar
 */
public class Document(
    public val namespace: String,
    public val definitions: List<Definition>,
) {

    public companion object {

        @JvmStatic
        @JvmOverloads
        public fun load(namespace: String, input: String, include: Path? = null): Document = Document(
            namespace = namespace,
            definitions = Parser.load(input, include),
        )
    }
}

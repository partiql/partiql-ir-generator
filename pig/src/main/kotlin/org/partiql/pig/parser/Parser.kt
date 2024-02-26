package org.partiql.pig.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.pig.antlr.RIDLBaseVisitor
import org.partiql.pig.antlr.RIDLLexer
import org.partiql.pig.antlr.RIDLParser
import org.partiql.pig.model.Definition
import org.partiql.pig.model.Operand
import org.partiql.pig.model.Scalar
import org.partiql.pig.model.Type
import java.io.ByteArrayInputStream
import java.nio.file.Path

/**
 * TODO
 */
internal object Parser {

    @JvmStatic
    fun load(input: String, include: Path?): List<Definition> {
        val source = ByteArrayInputStream(input.toByteArray(Charsets.UTF_8))
        val lexer = RIDLLexer(CharStreams.fromStream(source))
        lexer.removeErrorListeners()
        lexer.addErrorListener(LexerErrorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = RIDLParser(tokens)
        parser.reset()
        parser.removeErrorListeners()
        parser.addErrorListener(ParseErrorListener)
        val tree = parser.document()!!
        // 1st pass, build the symbol tree for name resolution.
        val symbols = Symbols.build(tree)
        // 2nd pass, populate definitions using the symbol tree.
        val definitions = mutableListOf<Definition>()
        DefinitionVisitor(symbols, definitions, symbols.root).visit(tree)
        return definitions
    }

    /**
     * Catches Lexical errors (unidentified tokens).
     */
    private object LexerErrorListener : BaseErrorListener() {

        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            if (offendingSymbol is Token) {
                val token = offendingSymbol.text
                // val tokenType = RIDLParser.VOCABULARY.getSymbolicName(offendingSymbol.type)
                val offset = charPositionInLine + 1
                val length = token.length
                throw IllegalArgumentException("$msg. Location $line:$offset:$length")
            } else {
                throw IllegalArgumentException("Offending symbol is not a Token.")
            }
        }
    }

    /**
     * Catches Parser errors (malformed syntax).
     */
    private object ParseErrorListener : BaseErrorListener() {

        private val rules = RIDLParser.ruleNames.asList()

        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            if (offendingSymbol is Token) {
                val rule = e?.ctx?.toString(rules) ?: "UNKNOWN"
                val token = offendingSymbol.text
                // val tokenType = RIDLParser.VOCABULARY.getSymbolicName(offendingSymbol.type)
                val offset = charPositionInLine + 1
                val length = token.length
                throw IllegalArgumentException("$msg. Rule: $rule, Location $line:$offset:$length")
            } else {
                throw IllegalArgumentException("Offending symbol is not a Token.")
            }
        }
    }

    private class DefinitionVisitor(
        private val symbols: Symbols,
        private val definitions: MutableList<Definition>,
        private val cursor: Symbol,
    ) : RIDLBaseVisitor<Type>() {

        override fun visitPrimitive(ctx: RIDLParser.PrimitiveContext): Type {
            val scalar = try {
                Scalar.valueOf(ctx.text.trim().uppercase())
            } catch (ex: IllegalArgumentException) {
                error("Unknown scalar `${ctx.text}`")
            }
            return Type.Primitive(scalar)
        }

        override fun visitList(ctx: RIDLParser.ListContext): Type {
            val t = visit(ctx.type())
            return Type.List(t)
        }

        override fun visitMap(ctx: RIDLParser.MapContext): Type {
            val k = visit(ctx.k)
            if (k !is Type.Primitive) {
                error("Map key must be a scalar")
            }
            val v = visit(ctx.v)
            return Type.Map(k.scalar, v)
        }

        override fun visitSet(ctx: RIDLParser.SetContext): Type {
            val t = visit(ctx.type())
            return Type.List(t)
        }

        override fun visitPathSymbol(ctx: RIDLParser.PathSymbolContext): Type {
            val name = ctx.NAME().text
            val node = cursor.find(name)
            if (node == null) {
                error("No definition found for symbol `$name`")
            }
            return Type.Path(node.path)
        }

        override fun visitPathRelative(ctx: RIDLParser.PathRelativeContext?): Type {
            TODO("Relative path not currently supported")
        }

        override fun visitPathAbsolute(ctx: RIDLParser.PathAbsoluteContext): Type {
            TODO("Absolute path not currently supported")
        }

        override fun visitProduct(ctx: RIDLParser.ProductContext): Type = scope(ctx.NAME()) {
            val path = it
            val operands = ctx.operand().map { op ->
                Operand(
                    label = op.NAME().text,
                    type = visitType(op.type())
                )
            }
            // Collect definitions in the scope as children.
            Definition.Product(path, operands, definitions)
        }

        override fun visitSum(ctx: RIDLParser.SumContext): Type = scope(ctx.NAME()) {
            val path = it
            // Descend in this scope
            ctx.variant().forEach { variant -> visitVariant(variant) }
            // Collect definitions in the scope as the sum variants.
            Definition.Sum(path, definitions)
        }

        override fun visitEnum(ctx: RIDLParser.EnumContext): Type = scope(ctx.NAME()) {
            val path = it
            val values = ctx.enumerators().enumerator().map { enumerator -> enumerator.text }
            Definition.Enum(path, values)
        }

        override fun visitFixed(ctx: RIDLParser.FixedContext): Type = scope(ctx.NAME()) {
            val path = it
            val size = ctx.int_().text.toInt()
            Definition.Fixed(path, size)
        }

        override fun visitUnit(ctx: RIDLParser.UnitContext): Type = scope(ctx.NAME()) {
            val path = it
            Definition.Unit(path)
        }

        private inline fun scope(symbol: TerminalNode, block: DefinitionVisitor.(path: Type.Path) -> Definition): Type {
            val name = symbol.text
            val child = cursor.children.first { it.name == name }
            val visitor = DefinitionVisitor(symbols, mutableListOf(), child)
            val path = Type.Path(child.path)
            val definition = visitor.block(path)
            definitions.add(definition)
            return path
        }
    }
}

package org.partiql.pig.tests

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.tests.generated.ToyLang
import kotlin.test.assertEquals

/**
 * Tests sum converters and demonstrates their use to convert ToyLang's AST into a text representation
 * that is vaguely similar to OCaml.  This example converts from a PIG domain sum type to a string, but these
 * conversions can be applied to just about anything.
 *
 * ToyLang's AST actually has two sum types: expr and operator.
 *
 * This example also demonstrates how converters for both can be used together.
 */
class SumConverterTests {

    private val toyOperatorConverter = object : ToyLang.Operator.Converter<String> {
        override fun convertPlus(node: ToyLang.Operator.Plus): String = "+"
        override fun convertMinus(node: ToyLang.Operator.Minus): String = "-"
        override fun convertTimes(node: ToyLang.Operator.Times): String = "*"
        override fun convertDivide(node: ToyLang.Operator.Divide): String = "/"
        override fun convertModulo(node: ToyLang.Operator.Modulo): String = "%"
    }

    private val toyExprConverter = object : ToyLang.Expr.Converter<String> {
        override fun convertLit(node: ToyLang.Expr.Lit): String = "${node.value}"
        override fun convertVariable(node: ToyLang.Expr.Variable): String = node.name.text
        override fun convertNot(node: ToyLang.Expr.Not): String = "!${convert(node.expr)}"
        override fun convertNary(node: ToyLang.Expr.Nary): String {
            // This Converter<T> implementation isn't responsible for converting ToyLang.Operator instances.
            // delegate to toyOperatorConverter for that.
            val op = toyOperatorConverter.convert(node.op)
            return node.operands.joinToString(" $op ") { convert(it) }
        }

        override fun convertLet(node: ToyLang.Expr.Let): String =
            "let ${node.name.text} = ${convert(node.value)} in ${convert(node.body)}"

        override fun convertFunction(node: ToyLang.Expr.Function): String =
            "fun (${node.varName.text}) -> ${convert(node.body)}"
    }

    data class TestCase(val ast: ToyLang.Expr, val expectedStringRepresentation: String)

    class Arguments : ArgumentsProviderBase() {
        private fun case(expectedStringRepresentation: String, block: ToyLang.Builder.() -> ToyLang.Expr): TestCase =
            TestCase(ast = block(ToyLang.BUILDER()), expectedStringRepresentation = expectedStringRepresentation)

        override fun getParameters() = listOf(
            case("x") { variable("x") },
            case("21 * 2") { nary(times(), lit(ionInt(21)), lit(ionInt(2))) },
            case("!!false") { not(not(lit(ionBool(false)))) },
            case("let x = 38 in x + 4") {
                let(
                    "x",
                    lit(ionInt(38)),
                    nary(plus(), variable("x"), lit(ionInt(4)))
                )
            },
            case("let x = 38 in x + 4") {
                let(
                    "x",
                    lit(ionInt(38)),
                    nary(plus(), variable("x"), lit(ionInt(4)))
                )
            },
            case("fun (n) -> n + 42") {
                function("n", nary(plus(), variable("n"), lit(ionInt(42))))
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun `convert toy AST to ocaml-like string`(tc: TestCase) {
        val converted = toyExprConverter.convert(tc.ast)

        assertEquals(tc.expectedStringRepresentation, converted)
    }
}

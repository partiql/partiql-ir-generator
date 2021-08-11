package org.partiql.pig.tests

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.tests.generated.ToyLang
import kotlin.test.assertEquals

/**
 * Tests sum converters and and demonstrates their use to convert ToyLang's AST into a text representation
 * that is vaguely similar to OCaml.  This example converts from a PIG domain sum type to a string, but these
 * conversions can be applied to just about anything.
 *
 * ToyLang's AST actually has two sum types: expr and operator.
 *
 * This example also demonstrates how both can be used together.
 */
class SumConverterTests {
val toyOperatorConverter = object : ToyLang.Operator.Converter<String> {
    override fun convertPlus(node: ToyLang.Operator.Plus): String = "+"
    override fun convertMinus(node: ToyLang.Operator.Minus): String = "-"
    override fun convertTimes(node: ToyLang.Operator.Times): String = "*"
    override fun convertDivide(node: ToyLang.Operator.Divide): String = "/"
    override fun convertModulo(node: ToyLang.Operator.Modulo): String = "%"
}

val toyExprConverter = object : ToyLang.Expr.Converter<String> {
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

    class TestCase(val ast: ToyLang.Expr, val expectedStringRepresentation: String)

    class Arguments : ArgumentsProviderBase(){
        override fun getParameters() = listOf(
            TestCase(
                ToyLang.build { variable("x") },
                "x"
            ),
            TestCase(
                ToyLang.build { nary(times(), lit(ionInt(21)), lit(ionInt(2))) },
                "21 * 2"
            ),
            TestCase(
                ToyLang.build { not(not(lit(ionBool(false)))) },
                "!!false"
            ),
            TestCase(
                ToyLang.build {
                    let(
                        "x",
                        lit(ionInt(38)),
                        nary(plus(), variable("x"), lit(ionInt(4))))
                },
                "let x = 38 in x + 4"
            ),
            TestCase(
                ToyLang.build {
                    let(
                        "x",
                        lit(ionInt(38)),
                        nary(plus(), variable("x"), lit(ionInt(4))))
                },
                "let x = 38 in x + 4"
            ),
            TestCase(
                ToyLang.build {
                    function("n", nary(plus(), variable("n"), lit(ionInt(42))))
                },
                "fun (n) -> n + 42"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun test1(tc: TestCase) {
        val converted = toyExprConverter.convert(tc.ast)

        assertEquals(tc.expectedStringRepresentation, converted)
    }
}


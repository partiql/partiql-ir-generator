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

package org.partiql.pig.tests

import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Test
import org.partiql.pig.tests.generated.ToyLang
import org.junit.jupiter.api.Assertions.assertEquals
import org.partiql.pig.runtime.LongPrimitive
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.tests.generated.TestDomain

const val INDEX_META_KEY = "index"

class VisitorTransformTests {

    /** The outermost visitor simply establishes the root [ScopeTracker] and recurses into it. */
    class VariableResolver : ToyLang.VisitorTransform() {

        override fun transformExprLetBody(node: ToyLang.Expr.Let): ToyLang.Expr =
            ScopeTracker(name = node.name.text, index = 0, parent = null)
                .transformExpr(node.body)

        /** This class actually does the variable resolution. */
        private class ScopeTracker(
            val name: String,
            val index: Int,
            val parent: ScopeTracker?
        ) : ToyLang.VisitorTransform() {

            /** Does the actual resolution of a variable's index. */
            override fun transformExprVariable(node: ToyLang.Expr.Variable): ToyLang.Expr.Variable =
                node.withMeta(INDEX_META_KEY, findIndex(node.name.text))

            /** Replaces the default behavior of [demo_ast_.VisitorBase] with a *nested* [ScopeTracker] visitor. */
            override fun transformExprLetBody(node: ToyLang.Expr.Let): ToyLang.Expr =
                createNested(node.name.text)
                    .transformExpr(node.body)

            /**
             * Recursively searches up the scope graph to find the variable with the specified [name].
             *
             * Throws if the variable is undefined.
             */
            private fun findIndex(name: String): Int =
                when {
                    this.name == name -> index
                    this.parent == null -> error("Undefined variable '$name'")
                    else -> this.parent.findIndex(name)
                }

            /** Creates a nested scope for the specified variable. */
            private fun createNested(name: String) =
                ScopeTracker(name, index + 1, this)
        }
    }

    @Test
    fun `demonstrate a simple variable index resolver`() { // also prove that metas can be added.

        /*
            Equivalent to:
            let foo = 42 in
                let bar = 48 in
                    foo + bar
         */
        val unresolved = ToyLang.build {
            let(
                "foo",
                lit(ionInt(42)),
                let(
                    "bar",
                    lit(ionInt(48)),
                    plus(variable("foo"), variable("bar"))))
        }

        val resolver = VariableResolver()
        val outerLet = resolver.transformExpr(unresolved) as ToyLang.Expr.Let

        val innerLet = outerLet.body as ToyLang.Expr.Let
        val plus = innerLet.body as ToyLang.Expr.Plus

        assertEquals(metaContainerOf(INDEX_META_KEY to 0), plus.operands[0].metas)
        assertEquals(metaContainerOf(INDEX_META_KEY to 1), plus.operands[1].metas)
    }

    private val longIncrementer = object : TestDomain.VisitorTransform() {
        override fun transformLongPrimitiveValue(sym: LongPrimitive): Long = sym.value + 1
    }

    @Test
    fun increment_intPair() {
        val input = TestDomain.build { intPair(1, 2) }
        val output = longIncrementer.transformIntPair(input)
        val expectedAst = TestDomain.build { intPair(2, 3) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_intPairPair() {
        val input = TestDomain.build { intPairPair(intPair(1, 3), intPair(5, 7)) }
        val output = longIncrementer.transformIntPairPair(input)
        val expectedAst = TestDomain.build { intPairPair(intPair(2, 4), intPair(6, 8)) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_domainLevelRecordSomeFieldWithOptionalField() {
        val input = TestDomain.build { domainLevelRecord(someField = 1, anotherField = "hi", optionalField = 3) }
        val output = longIncrementer.transformDomainLevelRecord(input)
        val expectedAst = TestDomain.build { domainLevelRecord(someField = 2, anotherField = "hi", optionalField = 4) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_domainLevelRecordSomeFieldWithoutOptionalField() {
        val input = TestDomain.build { domainLevelRecord(someField = 1, anotherField = "hi", optionalField = null) }
        val output = longIncrementer.transformDomainLevelRecord(input)
        val expectedAst = TestDomain.build { domainLevelRecord(someField = 2, anotherField = "hi", optionalField = null) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_testSumOne() {
        val input = TestDomain.build { one(1) }
        val output = longIncrementer.transformTestSum(input)
        val expectedAst = TestDomain.build { one(2) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_testSumTwo() {
        val input = TestDomain.build { two(1, 3) }
        val output = longIncrementer.transformTestSum(input)
        val expectedAst = TestDomain.build { two(2, 4) }
        assertEquals(expectedAst, output)
    }

    @Test
    fun increment_testSumThree() {
        val input = TestDomain.build { three(1, 3, 5) }
        val output = longIncrementer.transformTestSum(input)
        val expectedAst = TestDomain.build { three(2, 4, 6) }
        assertEquals(expectedAst, output)
    }

    private val nameMangler = object : TestDomain.VisitorTransform() {
        override fun transformEntityHumanFirstName(node: TestDomain.Entity.Human): SymbolPrimitive =
            SymbolPrimitive(node.firstName.text + "_mangled", node.firstName.metas)

        override fun transformEntityHumanLastName(node: TestDomain.Entity.Human): SymbolPrimitive =
            SymbolPrimitive(node.lastName.text + "_mangled", node.lastName.metas)
    }

    @Test
    fun mangleHumanNames() {
        val input = TestDomain.build { human(firstName = "Anakin", lastName = "Skywalker", title = "Jedi") }
        val output = nameMangler.transformEntity(input)
        val expectedAst = TestDomain.build { human(firstName = "Anakin_mangled", lastName = "Skywalker_mangled", title = "Jedi") }
        assertEquals(expectedAst, output)
    }
}


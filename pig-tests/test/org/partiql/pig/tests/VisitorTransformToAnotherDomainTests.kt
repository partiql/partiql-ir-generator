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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.partiql.pig.tests.generated.ToyLang
import org.partiql.pig.tests.generated.ToyLangNameless

class VisitorTransformToAnotherDomainTests {

    /**
     * This is a VisitorTransformToToyLangNameless implementation that transforms9
     * variable references from names to indexes.
     *
     * The input is an instance of the [ToyLang] domain model and the result is an instance of the
     * [ToyLangNameless] domain.
     */
    private class VariableResolverToPermutedDomain(
        val scope: Scope = Scope.Global
    ) : ToyLang.VisitorTransformToToyLangNameless() {

        /** Any variable reference resides in the top scope is undefined. */
        override fun transformExprVariable(node: ToyLang.Expr.Variable): ToyLangNameless.Expr {
            val thiz = this
            return ToyLangNameless.build {
                variable(index = thiz.scope.findIndex(node.name.text))
            }
        }

        override fun transformExprLet(node: ToyLang.Expr.Let): ToyLangNameless.Expr =
            ToyLangNameless.build {
                val nestedScope = this@VariableResolverToPermutedDomain.scope.nest(node.name.text)
                let(
                    index = nestedScope.index,
                    value = transformExpr(node.value),
                    body = VariableResolverToPermutedDomain(scope.nest(node.name.text)).transformExpr(node.body)
                )

            }
    }


    @Test
    fun `demonstrate a simple variable index resolver that transforms to a permuted domain`() {
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


        val resolver = VariableResolverToPermutedDomain()
        val outerLet = resolver.transformExpr(unresolved) as ToyLangNameless.Expr.Let

        val innerLet = outerLet.body as ToyLangNameless.Expr.Let
        val plus = innerLet.body as ToyLangNameless.Expr.Plus
        val operand1 = plus.operands[0] as ToyLangNameless.Expr.Variable
        val operand2 = plus.operands[1] as ToyLangNameless.Expr.Variable

        Assertions.assertEquals(0L, operand1.index.value)
        Assertions.assertEquals(1L, operand2.index.value)
    }

}
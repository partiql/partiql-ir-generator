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
import org.junit.jupiter.api.Test
import org.partiql.pig.runtime.asPrimitive
import org.partiql.pig.tests.generated.ToyLang
import org.partiql.pig.tests.generated.ToyLangIndexed
import org.partiql.pig.tests.generated.ToyLangIndexedToToyLangVisitorTransform
import org.partiql.pig.tests.generated.ToyLangToToyLangIndexedVisitorTransform
import kotlin.test.assertEquals

class PermuteTransformSmokeTests {
    /**
     * This is a VisitorTransformToToyLangNameless implementation that transforms variable references from names
     * to indexes.
     *
     * The input is an instance of the [ToyLang] domain model and the result is an instance of the
     * [ToyLangNameless] domain.
     */
    private class VariableResolverToPermutedDomain(
        val scope: Scope = Scope.Global
    ) : ToyLangToToyLangIndexedVisitorTransform() {

        /** Any variable reference that resides in the top scope is undefined. */
        override fun transformExprVariable(node: ToyLang.Expr.Variable): ToyLangIndexed.Expr {
            val thiz = this
            return ToyLangIndexed.build {
                variable_(name = node.name, index = thiz.scope.findIndex(node.name.text).asPrimitive())
            }
        }

        override fun transformExprLet(node: ToyLang.Expr.Let): ToyLangIndexed.Expr =
            ToyLangIndexed.build {
                val nestedScope = this@VariableResolverToPermutedDomain.scope.nest(node.name.text)

                let_(
                    name = node.name,
                    index = nestedScope.index.asPrimitive(),
                    value = transformExpr(node.value),
                    body = VariableResolverToPermutedDomain(scope.nest(node.name.text)).transformExpr(node.body))
            }
    }

    @Test
    fun `demonstrate a simple variable index resolver that transforms to a permuted domain and back again`() {
        /*
            Equivalent to:
            let foo = 42 in
                let bar = 48 in
                    foo + bar
         */
        val unindexed = ToyLang.build {
            let(
                "foo",
                lit(ionInt(42)),
                let(
                    "bar",
                    lit(ionInt(48)),
                    nary(plus(), variable("foo"), variable("bar"))))
        }


        val resolver = VariableResolverToPermutedDomain()
        val outerLet = resolver.transformExpr(unindexed) as ToyLangIndexed.Expr.Let

        val indexed = ToyLangIndexed.build {
            let(
                "foo",
                0,
                lit(ionInt(42)),
                let(
                    "bar",
                    1,
                    lit(ionInt(48)),
                    nary(plus(), variable("foo", 0), variable("bar", 1))))
        }
        assertEquals(indexed, outerLet)

        // We can also go the other direction

        val indexedToUnindexed = object : ToyLangIndexedToToyLangVisitorTransform() {
            override fun transformExprVariable(node: ToyLangIndexed.Expr.Variable): ToyLang.Expr =
                ToyLang.build {
                    variable_(
                        name = node.name,
                        metas = node.metas)
                }

            override fun transformExprLet(node: ToyLangIndexed.Expr.Let): ToyLang.Expr =
                ToyLang.build {
                    let_(
                        name = node.name,
                        value = transformExpr(node.value),
                        body = transformExpr(node.body),
                        metas = node.metas)
                }
        }

        val roundTripped = indexedToUnindexed.transformExpr(indexed)
        assertEquals(unindexed, roundTripped)
    }
}
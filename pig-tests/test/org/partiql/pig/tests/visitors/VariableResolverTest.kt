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

package org.partiql.pig.tests.visitors

import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Test
import org.partiql.pig.tests.generated.demo_ast
import kotlin.test.assertEquals

class VariableResolverTest {
    @Test
    fun demo1() {
        /*
            Equivalent to:

            let foo = 42 in
                let bar = 48 in
                    foo + bar
         */
        val unresolved = demo_ast.build {
            let("foo",
                literal(42),
                let("bar",
                    literal(48),
                    apply(plus(), variable("foo"), variable("bar"))))
        }

        val resolver = VariableResolver()
        val outerLet = resolver.visit_expr(unresolved) as demo_ast.expr.let

        val innerLet = outerLet.expr2 as demo_ast.expr.let
        val apply = innerLet.expr2 as demo_ast.expr.apply

        assertEquals(metaContainerOf(INDEX_META_KEY to 0), apply.expr1.metas)
        assertEquals(metaContainerOf(INDEX_META_KEY to 1), apply.expr2.metas)
    }
}
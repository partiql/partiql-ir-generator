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

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.pig.tests.generated.demo_ast

const val INDEX_META_KEY = "index"

/** The outermost visitor simply establishes the root [ScopeTracker] and recurses into it. */
class VariableResolver : demo_ast_.VisitorBase() {

    /** Replaces the default behavior of [demo_ast_.VisitorBase] with top-level [ScopeTracker] visitor. */
    override fun visit_expr_let_expr2(node: demo_ast.expr.let): demo_ast.expr {
        return ScopeTracker(name = node.symbol0.text, index = 0, parent = null)
            .visit_expr(node.expr2)
    }

    /** This class actually does the variable resolution. */
    private class ScopeTracker(
        val name: String,
        val index: Int,
        val parent: ScopeTracker?
    ) : demo_ast_.VisitorBase() {

        /**
         * Looks up the index of the variable and adds it as a meta to [node]'s meta collection.
         *
         * note that this method is invoked to rewrite the metas of `demo_ast.expr.variable` and that this
         * class isn't constructing new instances of `demo_ast.expr.variable` at all.  That's all being done
         * by the base class.  Constructing new nodes was a significant overhead of working with the old
         * `AstRewriterBase` class.
         */
        override fun visit_expr_variable_metas(node: demo_ast.expr.variable): MetaContainer =
            node.metas + metaContainerOf(INDEX_META_KEY to findIndex(node.symbol0.text))

        /** Replaces the default behavior of [demo_ast_.VisitorBase] with a *nested* [ScopeTracker] visitor. */
        override fun visit_expr_let_expr2(node: demo_ast.expr.let): demo_ast.expr =
            createNested(node.symbol0.text)
                .visit_expr(node.expr2)

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


/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.pig.parser

import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.pig.antlr.RIDLBaseVisitor
import org.partiql.pig.antlr.RIDLParser

internal class Symbols private constructor(val root: Symbol) {

    companion object {

        /**
         * Produce a tree of all symbols in the document.
         */
        @JvmStatic
        fun build(tree: RIDLParser.DocumentContext): Symbols {
            val root = Symbol(".")
            Visitor(root).visit(tree)
            return Symbols(root)
        }
    }

    private class Visitor(private val parent: Symbol) : RIDLBaseVisitor<Unit>() {

        override fun visitProduct(ctx: RIDLParser.ProductContext) {
            // link self to parent
            val child = Symbol(ctx.NAME().text, parent, mutableSetOf())
            // link parent to child
            parent.children.add(child)
            // descend
            val visitor = Visitor(child)
            ctx.definition().forEach { it.accept(visitor) }
        }

        override fun visitSum(ctx: RIDLParser.SumContext) {
            val child = Symbol(ctx.NAME().text, parent, mutableSetOf())
            parent.children.add(child)
            val visitor = Visitor(child)
            ctx.variant().forEach { it.accept(visitor) }
        }

        override fun visitEnum(ctx: RIDLParser.EnumContext): Unit = add(ctx.NAME())

        override fun visitFixed(ctx: RIDLParser.FixedContext): Unit = add(ctx.NAME())

        override fun visitUnit(ctx: RIDLParser.UnitContext): Unit = add(ctx.NAME())

        private fun add(name: TerminalNode) {
            val child = Symbol(name.text, parent, mutableSetOf())
            if (parent.children.contains(child)) {
                error("Definition `$parent` already contains a child definition `$child`")
            }
            parent.children.add(child)
        }
    }
}

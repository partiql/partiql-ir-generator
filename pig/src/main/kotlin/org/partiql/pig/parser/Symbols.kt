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

/**
 * Produce a graph of type identifiers from a list of type definitions.
 */
internal class Symbols private constructor(val root: Node) {

    companion object {

        @JvmStatic
        fun build(tree: RIDLParser.DocumentContext): Symbols {
            val root = Node("root")
            Visitor(root).visit(tree)
            return Symbols(root)
        }
    }

    private class Visitor(private val parent: Node) : RIDLBaseVisitor<Unit>() {

        override fun visitProduct(ctx: RIDLParser.ProductContext) {
            // link self to parent
            val child = Node(ctx.NAME().text, parent, mutableListOf())
            // link parent to child
            parent.children.add(child)
            // descend
            val visitor = Visitor(child)
            ctx.definition().forEach { it.accept(visitor) }
        }

        override fun visitSum(ctx: RIDLParser.SumContext) {
            val child = Node(ctx.NAME().text, parent, mutableListOf())
            parent.children.add(child)
            val visitor = Visitor(child)
            ctx.variant().forEach { it.accept(visitor) }
        }

        override fun visitEnum(ctx: RIDLParser.EnumContext): Unit = add(ctx.NAME())

        override fun visitFixed(ctx: RIDLParser.FixedContext): Unit = add(ctx.NAME())

        override fun visitUnit(ctx: RIDLParser.UnitContext): Unit = add(ctx.NAME())

        private fun add(name: TerminalNode) {
            val child = Node(name.text, parent, mutableListOf())
            parent.children.add(child)
        }
    }

    internal class Node(
        val name: String,
        val parent: Node? = null,
        val children: MutableList<Node> = mutableListOf()
    ) {

        val path: List<String>
            get() {
                val path = mutableListOf<String>()
                var node: Node? = this
                while (node != null) {
                    path.add(node.name)
                    node = node.parent
                }
                // Use [1, path.size) so that `_root` is excluded in the path
                return path.reversed().subList(1, path.size)
            }

        override fun toString() = path.joinToString(".")

        override fun hashCode() = path.hashCode()

        override fun equals(other: Any?) = when (other) {
            is Node -> this.path == other.path
            else -> false
        }
    }
}

package org.partiql.pig.parser

import java.util.LinkedList

/**
 * A symbol in the type definition tree. Used for name resolution.
 *
 * @property name
 * @property parent
 * @property children
 */
public class Symbol(
    public val name: String,
    public val parent: Symbol? = null,
    public val children: MutableSet<Symbol> = mutableSetOf()
) {

    public val path: Array<String>
        get() {
            val path = mutableListOf<String>()
            var curr: Symbol? = this
            while (curr != null) {
                path.add(curr.name)
                curr = curr.parent
            }
            // Use [1, path.size) so that `_root` is excluded in the path
            return path.reversed().subList(1, path.size).toTypedArray()
        }

    override fun toString(): String = path.joinToString(".")

    override fun hashCode(): Int = path.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        is Symbol -> this.path.contentEquals(other.path)
        else -> false
    }

    /**
     * Search for this name in this symbol's subtree.
     *
     *  1. Search down the subtree, returning the first match.
     *  2. Search up the tree, error if ambiguous.
     */
    public fun find(name: String): Symbol? {

        // Descend breadth first.
        val queue = LinkedList(children)
        while (queue.isNotEmpty()) {
            val curr = queue.pop()
            if (curr.name == name) {
                return curr
            }
            queue.addAll(curr.children)
        }

        // Look upwards, also breadth first, ignoring the current tree.
        var match: Symbol? = null
        val ignore = mutableListOf(this)
        queue.add(parent)
        while (queue.isNotEmpty()) {
            val curr = queue.pop()
            // Match?
            if (curr.name == name) {
                if (match == null) {
                    match = curr
                } else {
                    error("Ambiguous name, matched both `$match` and `$curr`")
                }
            }
            ignore.add(curr)
            if (curr.parent != null) {
                queue.add(parent)
            }
            queue.addAll(curr.children)
        }

        // Search is done, match may be null
        return match
    }
}

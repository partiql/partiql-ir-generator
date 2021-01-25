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

sealed class Scope {

    /** Creates a new scope nested within the current scope. */
    abstract fun nest(name: String): Lexical

    /**
     * Recursively searches up the scope graph to find the variable with the specified [name].
     *
     * Throws if the variable is undefined.
     */
    abstract fun findIndex(id: String): Long


    object Global : Scope() {
        override fun nest(name: String): Lexical = Lexical(name, 0, this)
        override fun findIndex(id: String): Long = error("Undefined variable '$id'")

        override fun toString(): String = "<globals>"
    }

    data class Lexical(val name: String, val index: Long, val parent: Scope) : Scope() {
        override fun nest(name: String): Lexical =
            Lexical(name, index + 1, this)

        override fun findIndex(id: String): Long =
            when (name) {
                id -> index
                else -> parent.findIndex(id)
            }
    }
}


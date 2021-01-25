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

package org.partiql.pig.domain.model

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer


/**
 * Base class for all PIG data types, including primitives and products, records and sums.
 */
sealed class DataType {
    abstract val metas: MetaContainer

    /**
     * Indicates if this data type is a primitive value.
     *
     * i.e. an int or symbol, and future data types such as boolean.
     *
     * This allows language targets to perform certain actions which only apply to these types but not to
     * other built-in types such as `ion`.
     */
    open val isPrimitive: Boolean = false

    /**
     * Indicates if this data type is built in to PIG or if it is a user-defined type.
     *
     * Built-in types include all primitive values and the `ion` ([DataType.Ion]) type.
     *
     * Knowing if a type is built-in or not allows for:
     *
     * - Semantic checking, i.e. cannot `remove` a built-in type from a permuted domain.
     * - During domain permutation, built-in types are skipped when computing the list of types that are members
     * of the resulting [TypeDomain].
     */
    open val isBuiltin: Boolean = false

    /**
     * The name of the data type.
     *
     * For all data types (built-in or user defined) this is the name of the type in PIG-latin.
     *
     * This is also the first element of an s-expression which identifies the user-defined product or
     * sum variant in an s-expression representation of a PIG node, i.e.: .  i.e. `lit` in `(lit 1)`.
     */
    abstract val tag: String

    /**
     * Represents an instance of the Ion DOM in the target language.
     */
    object Ion : DataType() {
        override val isPrimitive: Boolean get() = false
        override val isBuiltin: Boolean get() = true
        override val tag: String get() = "ion"
        override val metas: MetaContainer get() = emptyMetaContainer()
    }

    /**
     * Represents the equivalent of an Ion `int` in the target language.
     * This is one of pig's "primitive" types.
     */
    object Int : DataType() {
        override val isPrimitive: Boolean get() = true
        override val isBuiltin: Boolean get() = true
        override val tag: String get() = "int"
        override val metas: MetaContainer get() = emptyMetaContainer()
    }

    /** Represents the equivalent of an Ion `symbol` in the target language. */
    object Symbol : DataType() {
        override val isPrimitive: Boolean get() = true
        override val isBuiltin: Boolean get() = true
        override val tag: String get() = "symbol"
        override val metas: MetaContainer get() = emptyMetaContainer()
    }

    /** Represents a user-defined data type such as a product, sum or record. */
    sealed class UserType : DataType() {
        /**
         * If the domain in which this [UserType] is a member indicates the difference between two domains,
         * [isDifferent] will be true for user types that are different in the destination domain.
         */
        abstract val isDifferent: Boolean

        abstract fun copyAsDifferent(): UserType

        /**
         * A product type consisting of a [tag] and one or more [namedElements].
         *
         * A [Tuple] can be either a product or a record.  In the serialized form a product is a tuple whose
         * elements are not named while a record has named elements.
         */
        data class Tuple(
            override val tag: String,
            val tupleType: TupleType,
            val namedElements: List<NamedElement>,
            override val metas: MetaContainer,
            override val isDifferent: Boolean = false
        ) : UserType() {

            fun computeArity(): IntRange {
                // Calculate the arity range for this product... Due to type domain error checking,
                // we can make the following assumptions:
                // - There will never be more than one variadic field.
                // - If such a field exists, it will be the last field.
                return namedElements.fold(IntRange(0, 0)) { acc, curr ->
                    when (curr.typeReference.arity) {
                        Arity.Required -> IntRange(acc.first + 1, acc.last + 1)
                        Arity.Optional -> IntRange(acc.first, acc.last + 1)
                        is Arity.Variadic -> IntRange(acc.first, kotlin.Int.MAX_VALUE)
                    }
                }
            }

            override fun copyAsDifferent(): Tuple = this.copy(isDifferent = true)
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Tuple) return false

                if (tag != other.tag) return false
                if (tupleType != other.tupleType) return false
                if (namedElements != other.namedElements) return false
                if (isDifferent != other.isDifferent) return false
                // Metas intentionally omitted here
                return true
            }

            override fun hashCode(): kotlin.Int {
                var result = tag.hashCode()
                result = 31 * result + tupleType.hashCode()
                result = 31 * result + namedElements.hashCode()
                result = 31 * result + isDifferent.hashCode()
                // Metas intentionally omitted here
                return result
            }
        }

        /** A sum type consisting of a [tag] and one or more [variants]. */
        data class Sum(
            override val tag: String,
            val variants: List<Tuple>,
            override val metas: MetaContainer,
            override val isDifferent: Boolean = false
        ) : UserType() {

            override fun copyAsDifferent(): UserType = this.copy(isDifferent = true)
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Sum) return false

                if (tag != other.tag) return false
                if (variants != other.variants) return false
                if (isDifferent != other.isDifferent) return false
                // Metas intentionally omitted here

                return true
            }

            override fun hashCode(): kotlin.Int {
                var result = tag.hashCode()
                result = 31 * result + variants.hashCode()
                result = 31 * result + isDifferent.hashCode()
                // Metas intentionally omitted here
                return result
            }
        }
    }
}
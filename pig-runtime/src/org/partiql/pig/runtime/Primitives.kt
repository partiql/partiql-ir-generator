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

package org.partiql.pig.runtime

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.metaContainerOf
import com.amazon.ionelement.api.withMetas

class LongPrimitive(val value: Long, override val metas: MetaContainer) : DomainNode {
    override fun withMeta(key: String, value: Any): LongPrimitive =
        LongPrimitive(this.value, metas + metaContainerOf(key to value))

    override fun toIonElement(): IonElement = ionInt(value).withMetas(metas)
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongPrimitive

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class SymbolPrimitive(val text: String, override val metas: MetaContainer) : DomainNode {
    override fun withMeta(key: String, value: Any): SymbolPrimitive =
        SymbolPrimitive(text, metas + metaContainerOf(key to value))

    override fun toIonElement(): IonElement = ionSymbol(text).withMetas(metas)
    override fun toString(): String = text

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymbolPrimitive

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}

fun AnyElement.toLongPrimitive() =
    LongPrimitive(this.longValue, this.metas)

fun AnyElement.toSymbolPrimitive() =
    SymbolPrimitive(this.symbolValue, this.metas)

fun Long.asPrimitive(metas: MetaContainer = emptyMetaContainer()) =
    LongPrimitive(this, metas)

fun Int.asPrimitive(metas: MetaContainer = emptyMetaContainer()) =
    LongPrimitive(this.toLong(), metas)

fun String.asPrimitive(metas: MetaContainer = emptyMetaContainer()) =
    SymbolPrimitive(this, metas)

/** Converts the specified list of [Long] [values] to a list of [LongPrimitive]. */
fun listOfPrimitives(vararg values: Long) = values.map { it.asPrimitive() }

/** Converts the specified list of [Long] [values] to a list of [StringPrimitive]. */
fun listOfPrimitives(vararg values: String) = values.map { it.asPrimitive() }

/**
 * This function behaves identically to `listOf<LongPrimitive>()`.
 * 
 * This exists to reduce the complexity of the `kotlin.ftl` template.  Without it, the template would have to know 
 * if a call to `listOf<LongPrimitive>()` or `listOfPrimitives<Long>()` should be emitted.
 */
fun listOfPrimitives(vararg values: LongPrimitive) = values.toList()

/**
 * This function behaves identically to `listOf<SymbolPrimitive>()`.
 *
 * This exists to reduce the complexity of the `kotlin.ftl` template.  Without it, the template would have to know
 * if a call to `listOf<SymbolPrimitive>()` or `listOfPrimitives<Symbol>()` should be emitted.
 */
fun listOfPrimitives(vararg values: SymbolPrimitive) = values.toList()

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

package org.partiql.pig.legacy.runtime

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.metaContainerOf

/**
 * Represents a symbol value that is part of a generated type domain.
 *
 * This is needed to allow such values to have metas.
 */
class SymbolPrimitive(val text: String, override val metas: MetaContainer) : DomainNode {

    /** Creates a copy of the current node with the specified values. */
    fun copy(text: String = this.text, metas: MetaContainer = this.metas): SymbolPrimitive =
        SymbolPrimitive(text, metas)

    /** Creates a copy of the current [SymbolPrimitive] with [metas] as the new metas. */
    override fun copy(metas: MetaContainer): SymbolPrimitive =
        SymbolPrimitive(text, metas)

    /** Creates a copy of the current [SymbolPrimitive] with the specified additional meta. */
    override fun withMeta(metaKey: String, metaValue: Any): SymbolPrimitive =
        SymbolPrimitive(text, metas + metaContainerOf(metaKey to metaValue))

    /** Creates an [IonElement] representation of the current [SymbolPrimitive]. */
    override fun toIonElement(): IonElement = ionSymbol(text, metas = metas)

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

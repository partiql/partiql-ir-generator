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
import com.amazon.ionelement.api.MetaContainer

/**
 * Provides basic a overridable for metas, long and symbol primitives.
 *
 * This is the super class for domain-specific transformer base-classes.
 */
abstract class DomainVisitorTransformBase {
    open fun transformMetas(metas: MetaContainer) = metas

    open fun transformAnyElement(node: AnyElement): AnyElement {
        val newMetas = transformMetas(node.metas)
        return if(node.metas !== newMetas) {
            node.copy(metas = newMetas).asAnyElement()
        } else {
            node
        }
    }

    open fun transformSymbolPrimitive(sym: SymbolPrimitive): SymbolPrimitive {
        val newText = transformSymbolPrimitiveText(sym)
        val newMetas = transformSymbolPrimitiveMetas(sym)
        return if(sym.text != newText || sym.metas !== newMetas) {
            SymbolPrimitive(newText, newMetas)
        } else {
            sym
        }
    }

    open fun transformSymbolPrimitiveText(sym: SymbolPrimitive) = sym.text

    open fun transformSymbolPrimitiveMetas(sym: SymbolPrimitive) = transformMetas(sym.metas)

    open fun transformLongPrimitive(lng: LongPrimitive): LongPrimitive {
        val newValue = transformLongPrimitiveValue(lng)
        val newMetas = transformLongPrimitiveMetas(lng)
        return if(lng.value != newValue || lng.metas !== newMetas) {
            LongPrimitive(newValue, newMetas)
        } else {
            lng
        }
    }

    open fun transformLongPrimitiveValue(sym: LongPrimitive): Long = sym.value

    open fun transformLongPrimitiveMetas(sym: LongPrimitive) = transformMetas(sym.metas)

   open fun transformBoolPrimitive(b: BoolPrimitive): BoolPrimitive {
        val newValue = transformBoolPrimitiveValue(b)
        val newMetas = transformBoolPrimitiveMetas(b)
        return if(b.value != newValue || b.metas !== newMetas) {
            BoolPrimitive(newValue, newMetas)
        } else {
            b
        }
    }

    open fun transformBoolPrimitiveValue(sym: BoolPrimitive): Boolean = sym.value

    open fun transformBoolPrimitiveMetas(sym: BoolPrimitive) = transformMetas(sym.metas)

}


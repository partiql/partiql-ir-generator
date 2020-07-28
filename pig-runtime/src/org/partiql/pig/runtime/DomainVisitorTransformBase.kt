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

    open fun transformAnyElement(node: AnyElement): AnyElement =
        // TODO:  remove .asAnyElement() below when https://github.com/amzn/ion-element-kotlin/issues/36 is fixed.
        node.copy(metas = transformMetas(node.metas)).asAnyElement()

    open fun transformSymbolPrimitive(sym: SymbolPrimitive) =
        SymbolPrimitive(
            transformSymbolPrimitiveText(sym),
            transformSymbolPrimitiveMetas(sym))

    open fun transformSymbolPrimitiveText(sym: SymbolPrimitive) = sym.text

    open fun transformSymbolPrimitiveMetas(sym: SymbolPrimitive) = transformMetas(sym.metas)

    open fun transformLongPrimitive(lng: LongPrimitive) =
        LongPrimitive(
            transformLongPrimitiveValue(lng),
            transformLongPrimitiveMetas(lng))

    open fun transformLongPrimitiveValue(sym: LongPrimitive): Long = sym.value

    open fun transformLongPrimitiveMetas(sym: LongPrimitive) = transformMetas(sym.metas)

}


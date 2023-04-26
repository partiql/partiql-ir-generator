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

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.MetaContainer

open class DomainVisitorFoldBase<T> {

    protected open fun visitBoolPrimitive(node: org.partiql.pig.legacy.runtime.BoolPrimitive, accumulator: T): T =
        // default does nothing
        accumulator

    protected open fun visitLongPrimitive(node: LongPrimitive, accumulator: T): T =
        // default does nothing
        accumulator

    protected open fun visitSymbolPrimitive(node: SymbolPrimitive, accumulator: T): T =
        // default does nothing
        accumulator

    protected open fun visitAnyElement(node: AnyElement, accumulator: T): T =
        // default does nothing
        accumulator

    protected open fun visitMetas(node: MetaContainer, accumulator: T): T =
        // default does nothing
        accumulator

    // /////////////////////////////////////////////////////

    open fun walkBoolPrimitive(node: org.partiql.pig.legacy.runtime.BoolPrimitive, accumulator: T): T {
        val intermediate = visitBoolPrimitive(node, accumulator)
        return walkMetas(node.metas, intermediate)
    }

    open fun walkLongPrimitive(node: LongPrimitive, accumulator: T): T {
        val intermediate = visitLongPrimitive(node, accumulator)
        return walkMetas(node.metas, intermediate)
    }

    open fun walkSymbolPrimitive(node: SymbolPrimitive, accumulator: T): T {
        val intermediate = visitSymbolPrimitive(node, accumulator)
        return walkMetas(node.metas, intermediate)
    }

    open fun walkAnyElement(node: AnyElement, accumulator: T): T {
        var intermediate = visitAnyElement(node, accumulator)
        return walkMetas(node.metas, intermediate)
    }

    open fun walkMetas(node: MetaContainer, accumulator: T): T {
        return visitMetas(node, accumulator)
    }
}

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

open class DomainVisitorBase {

    protected open fun visitBoolPrimitive(node: org.partiql.pig.legacy.runtime.BoolPrimitive) {
        // default does nothing
    }

    protected open fun visitLongPrimitive(node: LongPrimitive) {
        // default does nothing
    }

    protected open fun visitSymbolPrimitive(node: SymbolPrimitive) {
        // default does nothing
    }

    protected open fun visitAnyElement(node: AnyElement) {
        // default does nothing
    }

    protected open fun visitMetas(metas: MetaContainer) {
        // default does nothing.
    }

    // /////////////////////////////////////////////////////

    open fun walkBoolPrimitive(node: org.partiql.pig.legacy.runtime.BoolPrimitive) {
        visitBoolPrimitive(node)
        walkMetas(node.metas)
    }

    open fun walkLongPrimitive(node: LongPrimitive) {
        visitLongPrimitive(node)
        walkMetas(node.metas)
    }

    open fun walkSymbolPrimitive(node: SymbolPrimitive) {
        visitSymbolPrimitive(node)
        walkMetas(node.metas)
    }

    open fun walkAnyElement(node: AnyElement) {
        visitAnyElement(node)
        walkMetas(node.metas)
    }

    open fun walkMetas(metas: MetaContainer) {
        visitMetas(metas)
    }
}

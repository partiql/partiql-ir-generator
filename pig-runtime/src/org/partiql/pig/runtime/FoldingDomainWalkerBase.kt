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

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer

open class FoldingDomainWalkerBase<V: FoldingDomainVisitorBase<T>, T>(
    protected val visitor: V
) {
    open fun walkLongPrimitive(node: LongPrimitive, accumulator: T): T {
        var current = accumulator
        current = visitor.visitLongPrimitive(node, current)
        return walkMetas(node.metas, current)
    }

    open fun walkSymbolPrimitive(node: SymbolPrimitive, accumulator: T): T {
        var current = accumulator
        current = visitor.visitSymbolPrimitive(node, current)
        return walkMetas(node.metas, current)
    }

    open fun walkIonElement(node: IonElement, accumulator: T): T {
        var current = accumulator
        current = visitor.visitIonElement(node, current)
        return walkMetas(node.metas, current)
    }

    open fun walkMetas(node: MetaContainer, accumulator: T): T {
        return visitor.visitMetas(node, accumulator)
    }
}


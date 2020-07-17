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

open class InspectingDomainVisitorBase {

    open fun visitLongPrimitive(node: LongPrimitive) {
        // default does nothing
    }

    open fun visitSymbolPrimitive(node: SymbolPrimitive) {
        // default does nothing
    }

    open fun visitIonElement(node: IonElement) {
        // default does nothing
    }

    open fun visitMetas(node: MetaContainer) {
        // default does nothing.
    }
}

open class InspectingDomainWalkerBase<T: InspectingDomainVisitorBase>(
    protected val visitor: T
) {
    open fun walkLongPrimitive(node: LongPrimitive) {
        visitor.visitLongPrimitive(node)
        walkMetas(node.metas)
    }

    open fun walkSymbolPrimitive(node: SymbolPrimitive) {
        visitor.visitSymbolPrimitive(node)
        walkMetas(node.metas)
    }

    open fun walkIonElement(node: IonElement) {
        visitor.visitIonElement(node)
        walkMetas(node.metas)
    }

    open fun walkMetas(node: MetaContainer) {
        visitor.visitMetas(node)
    }
}
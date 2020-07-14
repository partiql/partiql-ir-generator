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

import com.amazon.ionelement.api.MetaContainer

/**
 * Provides basic overridable for metas, long and symbol primitives.
 *
 * This is the super class for domain-specific visitor base-classes.
 * */
abstract class DomainVisitorBase {
    fun visitMetas(metas: MetaContainer) = metas

    open fun visitSymbol(sym: SymbolPrimitive) =
        SymbolPrimitive(
            visitSymbolText(sym),
            visitSymbolMetas(sym))

    open fun visitSymbolText(sym: SymbolPrimitive) = sym.text
    open fun visitSymbolMetas(sym: SymbolPrimitive) = visitMetas(sym.metas)

    open fun visitLong(lng: LongPrimitive) =
        LongPrimitive(
            visitLongValue(lng),
            visitLongMetas(lng))

    open fun visitLongValue(sym: LongPrimitive): Long = sym.value
    open fun visitLongMetas(sym: LongPrimitive) = visitMetas(sym.metas)
}


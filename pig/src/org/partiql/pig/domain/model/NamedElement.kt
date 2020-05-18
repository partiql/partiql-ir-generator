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
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol

/**
 * An element of a product or record.
 *
 * For products whose elements do not have names, the [name] field should be synthesized by the
 * instantiator.
 */
class NamedElement(val name: String, val typeReference: TypeRef, val metas: MetaContainer) {

    /**
     * Generates an s-expression representation of this [NamedElement].
     *
     * This primarily aids in unit testing and is not intended to have an identical structure to PIG-s type universe
     * syntax.
     */
    fun toIonElement() =
        ionSexpOf(
            ionSymbol(name),
            typeReference.toIonElement())
}
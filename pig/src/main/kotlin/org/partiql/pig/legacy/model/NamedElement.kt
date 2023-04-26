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

package org.partiql.pig.legacy.model

import com.amazon.ionelement.api.MetaContainer

/** An element of a product or record. */
data class NamedElement(
    /** The name of the element that should be used in generated code. */
    val identifier: String,
    /** The tag used in the s-expression representation, if this is a record element. */
    val tag: String,
    /** A reference to the type of this element.*/
    val typeReference: TypeRef,
    val metas: MetaContainer
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NamedElement) return false

        if (identifier != other.identifier) return false
        if (tag != other.tag) return false
        return typeReference == other.typeReference
        // Metas intentionally omitted here
    }

    override fun hashCode(): Int {
        var result = identifier.hashCode()
        result = 31 * result + tag.hashCode()
        result = 31 * result + typeReference.hashCode()
        // Metas intentionally omitted here

        return result
    }
}

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
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.metaContainerOf

/**
 * Represents a long value that is part of a generated type domain.
 *
 * This is needed to allow such values to have metas.
 */
class LongPrimitive(val value: Long, override val metas: MetaContainer) : DomainNode {

    /** Creates a copy of the current node with the specified values. */
    fun copy(value: Long = this.value, metas: MetaContainer = this.metas): LongPrimitive =
        LongPrimitive(value, metas)

    /** Creates a copy of the current [LongPrimitive] with [newMetas] as the new metas. */
    override fun copyMetas(newMetas: MetaContainer): LongPrimitive =
        LongPrimitive(value, newMetas)

    /** Creates a copy of the current [LongPrimitive] with the specified additional meta. */
    override fun withMeta(metaKey: String, metaValue: Any): LongPrimitive =
        LongPrimitive(this.value, metas + metaContainerOf(metaKey to metaValue))

    /** Creates an [IonElement] representation of the current [LongPrimitive]. */
    override fun toIonElement(): IonElement = ionInt(value, metas = metas)

    /** Converts [value] to a string. */
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongPrimitive

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}


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

/** All generated domain classes must implement this interface. */
interface DomainNode : MetaContainingNode {

    /** Creates a copy of the current node with [newMetas] as the new metas. */
    fun copyMetas(newMetas : MetaContainer): DomainNode

    /** Converts the current node to an instance of `IonElement`. */
    fun toIonElement(): IonElement

    /** This override narrows the return type of [MetaContainingNode.wtihMeta]. */
    override fun withMeta(metaKey: String, metaValue: Any): DomainNode

    /** Converts the current node to a String.  Most nodes should simply call `toIonElement().toString()`. */
    override fun toString(): String
}


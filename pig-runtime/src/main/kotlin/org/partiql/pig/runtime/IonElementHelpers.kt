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
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.SeqElement
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.location

/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun IonElement.toIonElement() = this

/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun String?.toIonElement() = this?.let { ionSymbol(this) } ?: ionNull()

/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun Long.toIonElement() = ionInt(this)

/**
 * Returns the string representation of the symbol in the first element of this container.
 *
 * If the first element is not a symbol, throws [IonElementException].
 * If this container has no elements, throws [MalformedDomainDataException].
 */
val SeqElement.tag: String get() = this.head.symbolValue

/**
 * Returns the first element of this container.
 *
 * If this container has no elements, throws [MalformedDomainDataException].
 */
val SeqElement.head: AnyElement
    get() =
        when (this.size) {
            0 -> errMalformed(this.metas.location, "Cannot get head of empty container")
            else -> this.values.first()
        }

/**
 * Returns a sub-list containing all elements of this container except the first.
 *
 * If this container has no elements, throws [MalformedDomainDataException].
 */
val SeqElement.tail: List<AnyElement>
    get() = when (this.size) {
        0 -> errMalformed(this.metas.location, "Cannot get tail of empty container")
        else -> this.values.subList(1, this.size)
    }

/** Returns the first element. */
val List<AnyElement>.head: AnyElement
    get() = this.first()

/** Returns a copy of the list with the first element removed. */
val List<AnyElement>.tail: List<AnyElement> get() = this.subList(1, this.size)

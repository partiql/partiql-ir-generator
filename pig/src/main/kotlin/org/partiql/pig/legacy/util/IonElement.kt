package org.partiql.pig.util

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.SeqElement

/**
 * Returns the string representation of the symbol in the first element of this container.
 *
 * If the first element is not a symbol, throws [IonElementException].
 * If this container has no elements, throws [NoSuchElementException].
 */
internal val SeqElement.tag: String get() = this.head.symbolValue

/**
 * Returns the first element of this container.
 *
 * If this container has no elements, throws [NoSuchElementException].
 */
internal val SeqElement.head: AnyElement
    get() = values.first()

/**
 * Returns a sub-list containing all elements of this container except the first.
 *
 * If this container has no elements, throws [NoSuchElementException].
 */
internal val SeqElement.tail: List<AnyElement> get() =
    when (this.size) {
        0 -> throw NoSuchElementException("Cannot get tail of empty container")
        else -> this.values.subList(1, this.size)
    }

/** Returns the first element. */
internal val List<AnyElement>.head: AnyElement
    get() = this.first()

/** Returns a copy of the list with the first element removed. */
internal val List<AnyElement>.tail: List<AnyElement> get() = this.subList(1, this.size)

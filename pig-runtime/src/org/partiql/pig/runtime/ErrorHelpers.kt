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
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.head
import com.amazon.ionelement.api.location

fun errMalformed(location: IonLocation?, message: String): Nothing =
    throw MalformedDomainDataException(location, message)

fun errUnexpectedField(location: IonLocation?, fieldName: String): Nothing =
    errMalformed(location, "Unexpected field '$fieldName' encountered")

fun SexpElement.requireArityOrMalformed(arity: Int) =
    requireArityOrMalformed(IntRange(arity, arity))

fun SexpElement.requireArityOrMalformed(arityRange: IntRange) {
    // Note: arity does not include the tag!
    val argCount = size - 1
    if(argCount !in arityRange) {
        errMalformed(
            metas.location,
            "$arityRange argument(s) were required to `${this.head}`, but $argCount was/were supplied.")
    }
}

/**
 * Returns a required argument of an s-expression, skipping the 'tag'.
 *
 * Given `(foo bar bat)`, when `i` is '0', this function will return `bar`.  When `i` is `1`, `bat` is returned.
 *
 * IonNull is used to indicate skipped optional argument. A [MalformedDomainDataException]
 * is thrown if IonNull is provided to a required argument. Only special case
 * in [getRequiredIon] below.
 */
fun SexpElement.getRequired(i: Int): AnyElement {
    argIndexInBoundOrMalformed(i)
    val ionElement = this.values[i + 1]
    return when {
        ionElement.isNull -> errMalformed(this.metas.location, "A non-null value is required.")
        else -> ionElement
    }
}

/**
 * Returns a required Ion-typed argument of an s-expression, skipping the 'tag'.
 *
 * Given `(foo bar bat)`, when `i` is '0', this function will return `bar`.  When `i` is `1`, `bat` is returned.
 *
 * This is a special case of [getRequired]. Generally, IonNull is not acceptable
 * for required arguments. But when getting a required Ion-type argument, IonNull
 * is considered a valid value and returned as it is.
 */
fun SexpElement.getRequiredIon(i: Int): IonElement {
    argIndexInBoundOrMalformed(i)
    return this.values[i + 1]
}

/**
 * Returns an optional argument of an s-expression, skipping the 'tag'.
 *
 * Given `(foo bar bat)`, when `i` is '0', this function will return `bar`.  When `i` is `1`, `bat` is returned.
 *
 * IonNull is used to indicate skipped optional argument. Return null if optional
 * argument is IonNull.
 */
fun SexpElement.getOptional(i: Int): AnyElement? {
    argIndexInBoundOrMalformed(i)
    val ionElement = this.values[i + 1]
    return when {
        ionElement.isNull -> null
        else -> ionElement
    }
}

private fun SexpElement.argIndexInBoundOrMalformed(i: Int) {
    if (i + 1 >= this.size)
        errMalformed(this.metas.location, "Argument index $i is out of bounds (max=${size - 2})")
}

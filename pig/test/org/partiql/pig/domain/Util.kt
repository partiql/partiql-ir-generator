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

package org.partiql.pig.domain

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonTextLocation
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.pig.domain.model.DataType
import org.partiql.pig.domain.model.PermutedDomain
import org.partiql.pig.domain.model.PermutedSum
import org.partiql.pig.domain.model.Statement
import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.errors.ErrorContext
import org.partiql.pig.errors.PigError

fun makeErr(line: Int, col: Int, errorContext: ErrorContext) =
    PigError(IonTextLocation(line.toLong(), col.toLong()), errorContext)

fun makeErr(errorContext: ErrorContext) =
    PigError(null, errorContext)


/*
 * The [toIonElement] functions below generate an s-expression representation of a [TypeUniverse].
 *
 * This primarily aids in unit testing and is not intended to have an identical structure to PIG's type universe
 * syntax.
 */


fun TypeUniverse.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("universe"),
        *statements.map { it.toIonElement() }.toTypedArray())


fun Statement.toIonElement(): IonElement = when(this) {
    is TypeDomain ->
        ionSexpOf(
            ionSymbol("domain"),
            ionSymbol(name),
            *userTypes.map { it.toIonElement() }.toTypedArray())
    is PermutedDomain ->
        ionSexpOf(
            ionSymbol("permute_domain"),
            ionSymbol(permutesDomain),
            ionSexpOf(
                ionSymbol("exclude"),
                *excludedTypes.map { ionSymbol(it) }.toTypedArray()),
            ionSexpOf(
                ionSymbol("include"),
                *includedTypes.map { it.toIonElement() }.toTypedArray()),
            ionSexpOf(
                ionSymbol("with"),
                *permutedSums.map { it.toIonElement() }.toTypedArray()))
}

fun PermutedSum.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("permuted_sum"),
        ionSymbol(tag),
        ionSexpOf(
            ionSymbol("remove"),
            *removedVariants.map { ionSymbol(it) }.toTypedArray()),
        ionSexpOf(
            ionSymbol("include"),
            *addedVariants.map { it.toIonElement() }.toTypedArray()))


fun DataType.toIonElement(): IonElement = when(this) {
    DataType.Ion -> ionSymbol("ion")
    DataType.Int -> ionSymbol("int")
    DataType.Symbol -> ionSymbol("symbol")
    is DataType.Tuple ->
        ionSexpOf(
            ionSymbol(tupleType.toString().toLowerCase()),
            ionSymbol(tag),
            *namedElements.map { it.toIonElement() }.toTypedArray())
    is DataType.Sum ->
        ionSexpOf(
            ionSymbol("sum"),
            ionSymbol(tag),
            *variants.map { it.toIonElement() }.toTypedArray())
}
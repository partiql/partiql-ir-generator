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
import com.amazon.ionelement.api.withAnnotations
import org.partiql.pig.domain.model.DataType
import org.partiql.pig.domain.model.NamedElement
import org.partiql.pig.domain.model.PermutedDomain
import org.partiql.pig.domain.model.PermutedSum
import org.partiql.pig.domain.model.Statement
import org.partiql.pig.domain.model.Transform
import org.partiql.pig.domain.model.TupleType
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
 */


fun TypeUniverse.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("universe"),
        *statements.map { it.toIonElement() }.toTypedArray())


fun Statement.toIonElement(): IonElement =
        when(this) {
            is TypeDomain ->
                ionSexpOf(
                    ionSymbol("define"),
                    ionSymbol(tag),
                    ionSexpOf(
                        ionSymbol("domain"),
                        *userTypes
                            .filterNot { it.isRemoved }
                            .map { it.toIonElement(includeTypeTag = true) }.toTypedArray()))
            is PermutedDomain ->
                ionSexpOf(
                        ionSymbol("define"),
                        ionSymbol(tag),
                        ionSexpOf(
                            listOf(
                                ionSymbol("permute_domain"),
                                ionSymbol(permutesDomain),
                                ionSexpOf(
                                    ionSymbol("exclude"),
                                    *excludedTypes.map { ionSymbol(it) }.toTypedArray()),
                                ionSexpOf(
                                    ionSymbol("include"),
                                    *includedTypes.map { it.toIonElement(includeTypeTag = true) }.toTypedArray())
                        ) + permutedSums.map { it.toIonElement() }))
            is Transform ->
                ionSexpOf(
                    ionSymbol("define"),
                    ionSymbol(this.name),
                    ionSexpOf(
                        ionSymbol("transform"),
                        ionSymbol(this.sourceDomainTag),
                        ionSymbol(this.destinationDomainTag)))
        }

fun PermutedSum.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("with"),
        ionSymbol(tag),
        ionSexpOf(
            ionSymbol("exclude"),
            *removedVariants.map { ionSymbol(it) }.toTypedArray()),
        ionSexpOf(
            ionSymbol("include"),
            *addedVariants.map { it.toIonElement(includeTypeTag = false) }.toTypedArray()))


fun DataType.toIonElement(includeTypeTag: Boolean): IonElement = when(this) {
    DataType.Ion -> ionSymbol("ion")
    DataType.Int -> ionSymbol("int")
    DataType.Symbol -> ionSymbol("symbol")
    is DataType.UserType.Tuple ->
        ionSexpOf(
            listOfNotNull(
                if(includeTypeTag) ionSymbol(tupleType.toString().toLowerCase()) else null,
                ionSymbol(tag),
                *namedElements.map { it.toIonElement(this.tupleType) }.toTypedArray()))
    is DataType.UserType.Sum ->
        ionSexpOf(
            ionSymbol("sum"),
            ionSymbol(tag),
            *variants
                .filterNot { it.isRemoved }
                .map { it.toIonElement(includeTypeTag = false) }.toTypedArray())
}

fun NamedElement.toIonElement(tupleType: TupleType) =
    when(tupleType) {
        TupleType.PRODUCT -> typeReference.toIonElement().withAnnotations(identifier)
        TupleType.RECORD ->
            ionSexpOf(ionSymbol(tag), typeReference.toIonElement()).let {
                when {
                    tag != identifier -> it.withAnnotations(identifier)
                    else -> it
                }
            }
    }

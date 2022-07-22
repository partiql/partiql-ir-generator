package org.partiql.pig.domain

import com.amazon.ionelement.api.IonElement
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

/*
 * The [toIonElement] functions below generate an s-expression representation of a [TypeUniverse].
 */

internal fun TypeUniverse.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("universe"),
        *statements.map { it.toIonElement() }.toTypedArray()
    )

internal fun Statement.toIonElement(): IonElement =
    when (this) {
        is TypeDomain ->
            ionSexpOf(
                ionSymbol("define"),
                ionSymbol(tag),
                ionSexpOf(
                    ionSymbol("domain"),
                    *userTypes
                        .filterNot { it.isDifferent }
                        .map { it.toIonElement(includeTypeTag = true) }.toTypedArray()
                )
            )
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
                            *excludedTypes.map { ionSymbol(it) }.toTypedArray()
                        ),
                        ionSexpOf(
                            ionSymbol("include"),
                            *includedTypes.map { it.toIonElement(includeTypeTag = true) }.toTypedArray()
                        )
                    ) + permutedSums.map { it.toIonElement() }
                )
            )
        is Transform ->
            ionSexpOf(
                ionSymbol("transform"),
                ionSymbol(this.sourceDomainTag),
                ionSymbol(this.destinationDomainTag)
            )
    }

internal fun PermutedSum.toIonElement(): IonElement =
    ionSexpOf(
        ionSymbol("with"),
        ionSymbol(tag),
        ionSexpOf(
            ionSymbol("exclude"),
            *removedVariants.map { ionSymbol(it) }.toTypedArray()
        ),
        ionSexpOf(
            ionSymbol("include"),
            *addedVariants.map { it.toIonElement(includeTypeTag = false) }.toTypedArray()
        )
    )

internal fun DataType.toIonElement(includeTypeTag: Boolean): IonElement = when (this) {
    DataType.Ion -> ionSymbol("ion")
    DataType.Bool -> ionSymbol("bool")
    DataType.Int -> ionSymbol("int")
    DataType.Symbol -> ionSymbol("symbol")
    is DataType.UserType.Tuple ->
        ionSexpOf(
            listOfNotNull(
                if (includeTypeTag) ionSymbol(tupleType.toString().toLowerCase()) else null,
                ionSymbol(tag),
                *namedElements.map { it.toIonElement(this.tupleType) }.toTypedArray()
            )
        )
    is DataType.UserType.Sum ->
        ionSexpOf(
            ionSymbol("sum"),
            ionSymbol(tag),
            *variants
                .filterNot { it.isDifferent }
                .map { it.toIonElement(includeTypeTag = false) }.toTypedArray()
        )
}

internal fun NamedElement.toIonElement(tupleType: TupleType) =
    when (tupleType) {
        TupleType.PRODUCT -> typeReference.toIonElement().withAnnotations(identifier)
        TupleType.RECORD ->
            ionSexpOf(ionSymbol(tag), typeReference.toIonElement()).let {
                when {
                    tag != identifier -> it.withAnnotations(identifier)
                    else -> it
                }
            }
    }

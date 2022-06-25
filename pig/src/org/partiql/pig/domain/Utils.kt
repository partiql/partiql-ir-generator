package org.partiql.pig.domain

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.withAnnotations
import org.partiql.pig.domain.model.DataType
import org.partiql.pig.domain.model.NamedElement
import org.partiql.pig.domain.model.TupleType
import org.partiql.pig.domain.model.TypeDomain

/**
 *  Filter a [List]<[TypeDomain]> to only contain [TypeDomain]s that are present in [domains]
 */
fun List<TypeDomain>.filterDomains(domains: Set<String>?) =
    domains?.let { this.filter { domain -> domain.tag in it } } ?: this

/*
 * The [toIonElement] functions below generate an s-expression representation of a [TypeDomain].
 */

fun TypeDomain.toIonElement(): IonElement =
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

fun DataType.toIonElement(includeTypeTag: Boolean): IonElement = when (this) {
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

fun NamedElement.toIonElement(tupleType: TupleType) =
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

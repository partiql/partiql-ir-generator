package org.partiql.pig.domain.model

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.withAnnotations

/*
 * The [toIonElement] functions below generate an s-expression representation of a [TypeUniverse].
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

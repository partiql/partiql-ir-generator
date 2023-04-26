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

package org.partiql.pig.legacy.parser

import com.amazon.ion.IonReader
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.pig.legacy.model.Arity
import org.partiql.pig.legacy.model.DataType
import org.partiql.pig.legacy.model.NamedElement
import org.partiql.pig.legacy.model.PermutedDomain
import org.partiql.pig.legacy.model.PermutedSum
import org.partiql.pig.legacy.model.Statement
import org.partiql.pig.legacy.model.Transform
import org.partiql.pig.legacy.model.TupleType
import org.partiql.pig.legacy.model.TypeAnnotation
import org.partiql.pig.legacy.model.TypeDomain
import org.partiql.pig.legacy.model.TypeRef
import org.partiql.pig.legacy.model.TypeUniverse
import org.partiql.pig.legacy.util.head
import org.partiql.pig.legacy.util.tag
import org.partiql.pig.legacy.util.tail

/** Parses a type universe contained in [universeText]. */
fun parseTypeUniverse(universeText: String) =
    IonReaderBuilder.standard().build(universeText).use {
        parseTypeUniverse(it)
    }

/** Parses a type universe in the specified [IonReader]. */
fun parseTypeUniverse(reader: IonReader): TypeUniverse {
    val elementLoader = createIonElementLoader(IonElementLoaderOptions(includeLocationMeta = true))

    val domains = try {
        val topLevelElements = elementLoader.loadAllElements(reader)
        topLevelElements.map { topLevelValue ->
            val topLevelSexp = topLevelValue.asSexp()
            when (topLevelSexp.tag) {
                "define" -> parseDefine(topLevelSexp)
                "transform" -> parseTransform(topLevelSexp)
                else -> parseError(
                    topLevelSexp.head,
                    ParserErrorContext.InvalidTopLevelTag(topLevelSexp.tag)
                )
            }
        }
    } catch (iee: IonElementException) {
        parseError(iee.location, ParserErrorContext.IonElementError(iee))
    }

    return TypeUniverse(domains)
}

private fun parseDefine(sexp: SexpElement): Statement {
    requireArityForTag(sexp, 2)
    val args = sexp.tail // Skip tag
    val name = args.head.symbolValue
    val valueSexp = args.tail.head.asSexp()

    return when (valueSexp.tag) {
        "domain" -> parseTypeDomain(name, valueSexp)
        "permute_domain" -> parsePermuteDomain(name, valueSexp)
        else -> parseError(
            valueSexp.head,
            ParserErrorContext.UnknownConstructor(valueSexp.tag)
        )
    }
}

fun parseTransform(sexp: SexpElement): Statement {
    requireArityForTag(sexp, 2)
    return Transform(
        sourceDomainTag = sexp.values[1].symbolValue,
        destinationDomainTag = sexp.values[2].symbolValue,
        metas = sexp.metas
    )
}

private fun parseTypeDomain(domainName: String, sexp: SexpElement): TypeDomain {
    val args = sexp.tail // Skip tag
    // val typesSexps = args.tail

    val userTypes = args.map { tlv ->
        val tlvs = tlv.asSexp()
        parseDomainLevelStatement(tlvs)
    }.toList()

    return TypeDomain(
        tag = domainName,
        userTypes = userTypes,
        metas = sexp.metas
    )
}

private fun parseDomainLevelStatement(sexp: SexpElement): DataType.UserType {
    if (sexp.annotations.size > 1) {
        parseError(sexp, ParserErrorContext.MultipleTypeAnnotations)
    }
    val annotations = sexp.annotations.mapNotNull { TypeAnnotation.of(it) }
    return when (sexp.tag) {
        "product" -> parseProductBody(sexp.tail, sexp.metas, annotations)
        "record" -> parseRecordBody(sexp.tail, sexp.metas, annotations)
        "sum" -> parseSum(sexp, annotations)
        else -> parseError(sexp.head, ParserErrorContext.InvalidDomainLevelTag(sexp.tag))
    }
}

// Parses a sum-variant product or record (depending on the syntax used)
private fun parseVariant(
    bodyArguments: List<AnyElement>,
    metas: MetaContainer,
    annotations: List<TypeAnnotation>
): DataType.UserType.Tuple {
    val elements = bodyArguments.tail

    // If there are no elements, definitely not a record.
    val isRecord = if (elements.none()) {
        false
    } else {
        // if the head element is an s-exp that does not start with `?` or `*` then we're parsing a record
        when (val headElem = elements.head) {
            is SexpElement -> {
                when (headElem.values.head.symbolValue) {
                    "?", "*" -> false
                    else -> true
                }
            }
            is SymbolElement -> false
            else -> parseError(elements.head, ParserErrorContext.ExpectedSymbolOrSexp(elements.head.type))
        }
    }

    return when {
        isRecord -> {
            parseRecordBody(bodyArguments, metas, annotations)
        } else -> {
            parseProductBody(bodyArguments, metas, annotations)
        }
    }
}

private fun parseProductBody(args: List<AnyElement>, metas: MetaContainer, annotations: List<TypeAnnotation>) =
    DataType.UserType.Tuple(
        tag = args.head.symbolValue,
        tupleType = TupleType.PRODUCT,
        namedElements = parseProductElements(args.tail),
        metas = metas,
        annotations = annotations
    )

private fun parseProductElements(values: List<IonElement>): List<NamedElement> =
    values.map {
        val identifier = when (it.annotations.size) {
            // TODO: add tests for these errrors
            0 -> parseError(it, ParserErrorContext.MissingElementIdentifierAnnotation)
            1 -> it.annotations.single()
            else -> parseError(it, ParserErrorContext.MultipleElementIdentifierAnnotations)
        }

        NamedElement(
            tag = "", // NOTE: tag is not used in the s-expression representation of products!
            identifier = identifier,
            typeReference = parseSingleTypeRef(it),
            metas = it.metas
        )
    }

private fun parseRecordBody(bodyArguments: List<AnyElement>, metas: MetaContainer, annotations: List<TypeAnnotation>) =
    DataType.UserType.Tuple(
        tag = bodyArguments.head.symbolValue,
        tupleType = TupleType.RECORD,
        namedElements = parseRecordElements(bodyArguments.tail),
        metas = metas,
        isDifferent = false,
        annotations = annotations
    )

fun parseRecordElements(elementSexps: List<AnyElement>): List<NamedElement> =
    elementSexps.asSequence()
        .map { it.asSexp() }
        .map { elementSexp ->
            if (elementSexp.values.size != 2) {
                parseError(elementSexp, ParserErrorContext.InvalidArity(2, elementSexp.size))
            }
            val tag = elementSexp.values[0].symbolValue
            val identifier = when (elementSexp.annotations.size) {
                0 -> tag
                1 -> elementSexp.annotations.single()
                else -> parseError(elementSexp, ParserErrorContext.MultipleElementIdentifierAnnotations)
            }
            val typeRef = parseSingleTypeRef(elementSexp.values[1])
            NamedElement(
                identifier = identifier,
                tag = tag,
                typeReference = typeRef,
                metas = elementSexp.metas
            )
        }
        .toList()

private fun parseSum(sexp: SexpElement, annotations: List<TypeAnnotation>) =
    DataType.UserType.Sum(
        tag = sexp.tail.head.symbolValue,
        variants = sexp.tail.tail.map { parseSumVariant(it.asSexp()) },
        metas = sexp.metas,
        isDifferent = false,
        annotations = annotations
    )

private fun parseSumVariant(sexp: SexpElement): DataType.UserType.Tuple {
    if (sexp.annotations.size > 1) {
        parseError(sexp, ParserErrorContext.MultipleTypeAnnotations)
    }
    val annotations = sexp.annotations.mapNotNull { TypeAnnotation.of(it) }
    return parseVariant(sexp.values, sexp.metas, annotations)
}

private fun parseSingleTypeRef(typeRefExp: IonElement): TypeRef {
    val metas = typeRefExp.metas
    return when (typeRefExp) {
        is SymbolElement -> TypeRef(typeRefExp.textValue, Arity.Required, metas)
        is SexpElement -> {
            when (typeRefExp.tag) {
                "?" -> {
                    requireArityForTag(typeRefExp, 1)
                    val typeName = typeRefExp.tail.head.symbolValue
                    TypeRef(typeName, Arity.Optional, metas)
                }
                "*" -> {
                    requireArityForTag(typeRefExp, IntRange(2, 3))
                    val typeName = typeRefExp.tail.head.symbolValue
                    val arityRange = typeRefExp.tail.tail
                    val minArity = arityRange.head.longValue
                    TypeRef(typeName, Arity.Variadic(minArity.toInt()), metas)
                }
                else -> parseError(typeRefExp.head, ParserErrorContext.ExpectedTypeReferenceArityTag(typeRefExp.tag))
            }
        }
        else -> parseError(typeRefExp, ParserErrorContext.ExpectedSymbolOrSexp(typeRefExp.type))
    }
}

private fun parsePermuteDomain(domainName: String, sexp: SexpElement): PermutedDomain {
    val args = sexp.tail // Skip tag

    val permutingDomain = args.head.symbolValue
    val removedTypes = mutableListOf<String>()
    val newTypes = mutableListOf<DataType.UserType>()
    val permutedSums = mutableListOf<PermutedSum>()

    val alterSexps = args.tail
    alterSexps.map { it.asSexp() }.forEach { alterSexp ->
        when (alterSexp.head.symbolValue) {
            "with" -> permutedSums.add(parseWithSum(alterSexp))
            "exclude" -> alterSexp.tail.mapTo(removedTypes) { it.symbolValue }
            "include" -> alterSexp.tail.mapTo(newTypes) { parseDomainLevelStatement(it.asSexp()) }
            else -> parseError(alterSexp, ParserErrorContext.InvalidPermutedDomainTag(alterSexp.head.symbolValue))
        }
    }

    return PermutedDomain(
        tag = domainName,
        permutesDomain = permutingDomain,
        excludedTypes = removedTypes,
        includedTypes = newTypes,
        permutedSums = permutedSums,
        metas = sexp.metas
    )
}

private fun parseWithSum(sexp: SexpElement): PermutedSum {
    val args = sexp.tail // Skip tag

    val nameOfAlteredSum = args.head.symbolValue
    val removedVariants = mutableListOf<String>()
    val addedVariants = mutableListOf<DataType.UserType.Tuple>()

    args.tail.forEach { alterationValue ->
        val alterationSexp = alterationValue.asSexp()
        when (val alterationTag = alterationSexp.tag) {
            "exclude" -> alterationSexp.tail.mapTo(removedVariants) { it.symbolValue }
            "include" -> alterationSexp.tail.mapTo(addedVariants) { parseSumVariant(it.asSexp()) }
            else -> parseError(alterationSexp, ParserErrorContext.InvalidWithSumTag(alterationTag))
        }
    }

    return PermutedSum(nameOfAlteredSum, removedVariants, addedVariants, sexp.metas)
}

private fun requireArityForTag(sexp: SexpElement, arity: Int) {
    // Note: arity does not include the tag!
    val argCount = sexp.values.size - 1
    if (argCount != arity) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(IntRange(arity, arity), sexp.head.symbolValue, argCount))
    }
}

private fun requireArityForTag(sexp: SexpElement, arityRange: IntRange) {
    // Note: arity does not include the tag!
    val argCount = sexp.values.size - 1
    if (argCount !in arityRange) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(arityRange, sexp.head.symbolValue, argCount))
    }
}

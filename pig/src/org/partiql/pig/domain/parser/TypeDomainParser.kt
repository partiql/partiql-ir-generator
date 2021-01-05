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

package org.partiql.pig.domain.parser

import com.amazon.ionelement.api.*
import com.amazon.ion.IonReader
import com.amazon.ion.system.IonReaderBuilder
import org.partiql.pig.domain.model.*

/** Parses a type universe contained in [universeText]. */
fun parseTypeUniverse(universeText: String) =
    IonReaderBuilder.standard().build(universeText).use {
        parseTypeUniverse(it)
    }

/** Parses a type universe in the specified [IonReader]. */
fun parseTypeUniverse(reader: IonReader): TypeUniverse {
    val elementLoader = createIonElementLoader(IonElementLoaderOptions(includeLocationMeta = true))

    val domains = try {
        val idom = elementLoader.loadAllElements(reader)
        idom.map { topLevelValue ->
            val topLevelSexp = topLevelValue.asSexp()
            when (topLevelSexp.tag) {
                "define" -> parseDefine(topLevelSexp)
                else -> parseError(
                    topLevelSexp.head,
                    ParserErrorContext.InvalidTopLevelTag(topLevelSexp.tag))
            }
        }
    }
    catch(iee: IonElementException) {
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
        "transform" -> parseTransform(name, valueSexp)
        else -> parseError(
            valueSexp.head,
            ParserErrorContext.UnknownConstructor(valueSexp.tag))
    }
}

fun parseTransform(name: String, sexp: SexpElement): Statement {
    requireArityForTag(sexp, 2)
    return Transform(
        name = name,
        sourceDomain = sexp.values[1].symbolValue,
        destinationDomain = sexp.values[2].symbolValue,
        metas = sexp.metas
    )
}

private fun parseTypeDomain(domainName: String, sexp: SexpElement): TypeDomain {
    val args = sexp.tail // Skip tag
    //val typesSexps = args.tail

    val userTypes = args.map { tlv ->
        val tlvs = tlv.asSexp()
        parseDomainLevelStatement(tlvs)
    }.toList()

    return TypeDomain(
        domainName,
        userTypes,
        sexp.metas)
}

private fun parseDomainLevelStatement(tlvs: SexpElement): DataType {
    return when (tlvs.tag) {
        "product" -> parseProductBody(tlvs.tail, tlvs.metas)
        "record" -> parseRecordBody(tlvs.tail, tlvs.metas)
        "sum" -> parseSum(tlvs)
        else -> parseError(tlvs.head, ParserErrorContext.InvalidDomainLevelTag(tlvs.tag))
    }
}

private fun parseTypeRefs(values: List<IonElement>): List<TypeRef> =
    values.map { parseSingleTypeRef(it) }

// Parses a sum-variant product or record (depending on the syntax used)
private fun parseVariant(
    bodyArguments: List<AnyElement>,
    metas: MetaContainer
): DataType.Tuple {
    val elements = bodyArguments.tail

    // If there are no elements, definitely not a record.
    val isRecord = if(elements.none()) {
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
            parseRecordBody(bodyArguments, metas)
        } else -> {
            parseProductBody(bodyArguments, metas)
        }
    }
}

private fun parseProductBody(bodyArguments: List<AnyElement>, metas: MetaContainer): DataType.Tuple {
    val typeName = bodyArguments.head.symbolValue

    val namedElements = parseProductElements(bodyArguments.tail)

    return DataType.Tuple(typeName, TupleType.PRODUCT, namedElements, metas)
}

private fun parseProductElements(values: List<IonElement>): List<NamedElement> =
    values.map {
        val identifier = when(it.annotations.size) {
            // TODO: add tests for these errrors
            0 -> parseError(it, ParserErrorContext.MissingElementIdentifierAnnotation)
            1 -> it.annotations.single()
            else -> parseError(it, ParserErrorContext.MultipleElementIdentifierAnnotations)
        }

        NamedElement(
            tag = "",  // NOTE: tag is not used in the s-expression representation of products!
            identifier = identifier,
            typeReference = parseSingleTypeRef(it),
            metas = it.metas)
    }

private fun parseRecordBody(bodyArguments: List<AnyElement>, metas: MetaContainer): DataType.Tuple {
    val typeName = bodyArguments.head.symbolValue
    val namedElements = parseRecordElements(bodyArguments.tail)
    return DataType.Tuple(typeName, TupleType.RECORD, namedElements, metas)
}

fun parseRecordElements(elementSexps: List<AnyElement>): List<NamedElement> =
    elementSexps.asSequence()
        .map { it.asSexp() }
        .map { elementSexp ->
            if(elementSexp.values.size != 2) {
                parseError(elementSexp, ParserErrorContext.InvalidArity(2, elementSexp.size))
            }
            val tag = elementSexp.values[0].symbolValue
            val identifier = when(elementSexp.annotations.size) {
                0 -> tag
                1 -> elementSexp.annotations.single()
                else -> parseError(elementSexp, ParserErrorContext.MultipleElementIdentifierAnnotations)
            }
            val typeRef = parseSingleTypeRef(elementSexp.values[1])
            NamedElement(
                identifier = identifier,
                tag = tag,
                typeReference = typeRef,
                metas = elementSexp.metas)
        }
        .toList()

private fun parseSum(sexp: SexpElement): DataType.Sum {
    val args = sexp.tail // Skip tag
    val typeName = args.head.symbolValue

    val variants = args.tail.map {
        parseSumVariant(it.asSexp())
    }

    return DataType.Sum(typeName, variants.toList(), sexp.metas)
}

private fun parseSumVariant(sexp: SexpElement): DataType.Tuple {
    return parseVariant(sexp.values, sexp.metas)
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
    val newTypes = mutableListOf<DataType>()
    val permutedSums = mutableListOf<PermutedSum>()

    val alterSexps = args.tail
    alterSexps.map { it.asSexp() }.forEach { alterSexp ->
        when(alterSexp.head.symbolValue) {
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
        metas = sexp.metas)
}

private fun parseWithSum(sexp: SexpElement): PermutedSum {
    val args = sexp.tail // Skip tag

    val nameOfAlteredSum = args.head.symbolValue
    val removedVariants = mutableListOf<String>()
    val addedVariants = mutableListOf<DataType.Tuple>()

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
    if(argCount != arity) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(IntRange(arity, arity), sexp.head.symbolValue, argCount))
    }
}

private fun requireArityForTag(sexp: SexpElement, arityRange: IntRange) {
    // Note: arity does not include the tag!
    val argCount = sexp.values.size - 1
    if(argCount !in arityRange) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(arityRange, sexp.head.symbolValue, argCount))
    }
}

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
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import org.partiql.pig.domain.model.*
/** Parses a type universe contained in [universeText]. */
fun parseTypeUniverse(universeText: String) =
    IonReaderBuilder.standard().build(universeText).use {
        parseTypeUniverse(it)
    }

/** Parses a type universe in the specified [IonReader]. */
fun parseTypeUniverse(reader: IonReader): TypeUniverse {
    val elementLoader = createIonElementLoader(includeLocations = true)

    val domains = try {
        val idom = elementLoader.loadAllElements(reader)
        idom.map { topLevelValue ->
            val topLevelSexp = topLevelValue.sexpValue
            when (topLevelSexp.tag) {
                "define" -> parseDefine(topLevelSexp)
                else -> parseError(
                    topLevelSexp.head,
                    ParserErrorContext.InvalidTopLevelTag(topLevelSexp.tag))
            }
        }
    }
    catch(iee: IonElectrolyteException) {
        parseError(iee.location, ParserErrorContext.IonElementError(iee))
    }

    return TypeUniverse(domains)
}

private fun parseDefine(sexp: IonElementContainer): Statement {
    requireArityForTag(sexp, 2)
    val args = sexp.tail // Skip tag
    val name = args.head.symbolValue
    val valueSexp = args.tail.head.sexpValue

    return when (valueSexp.tag) {
        "domain" -> parseTypeDomain(name, valueSexp)
        "permute_domain" -> parsePermuteDomain(name, valueSexp)
        else -> parseError(
            valueSexp.head,
            ParserErrorContext.UnknownConstructor(valueSexp.tag))
    }
}

private fun parseTypeDomain(domainName: String, sexp: IonElementContainer): TypeDomain {
    val args = sexp.tail // Skip tag
    //val typesSexps = args.tail

    val userTypes = args.map { tlv ->
        val tlvs = tlv.sexpValue
        parseDomainLevelStatement(tlvs)
    }.toList()

    return TypeDomain(
        domainName,
        userTypes,
        sexp.metas)

}

private fun parseDomainLevelStatement(tlvs: IonElementContainer): DataType {
    return when (tlvs.tag) {
        "product" -> parseDomainProduct(tlvs)
        "sum" -> parseSum(tlvs)
        else -> parseError(tlvs.head, ParserErrorContext.InvalidDomainLevelTag(tlvs.tag))
    }
}

private fun parseTypeRefs(values: List<IonElement>): List<TypeRef> =
    values.map { parseSingleTypeRef(it) }

private fun parseDomainProduct(sexp: IonElementContainer): DataType.Tuple {
    val args = sexp.tail // Skip tag

    return parseTupleBody(args, sexp.metas)
}

private fun parseTupleBody(
    bodyArguments: List<IonElement>,
    metas: MetaContainer
): DataType.Tuple {
    val typeName = bodyArguments.head.symbolValue
    val elements = bodyArguments.tail

    // If there are no elements, definitely not a record.
    val isRecord = if(elements.none()) {
        false
    } else {
        // if the head element is an s-exp that does not start with `?` or `*` then we're parsing a record
        when (elements.head.type) {
            IonType.SEXP -> {
                when (elements.head.sexpValue.head.symbolValue) {
                    "?", "*" -> false
                    else -> true
                }
            }
            IonType.SYMBOL -> false
            else -> parseError(elements.head, ParserErrorContext.ExpectedSymbolOrSexp(elements.head.type))
        }
    }

    return when {
        isRecord -> {
            val namedElements = parseNamedElements(bodyArguments.tail)
            DataType.Tuple(typeName, TupleType.RECORD, namedElements, metas)
        } else -> {
            val types = parseTypeRefs(bodyArguments.tail)

            // Synthesize names for the un-named elements here...
            val namedElements = types.mapIndexed { i, t ->
                NamedElement("${t.typeName}$i", t, t.metas)
            }

            DataType.Tuple(typeName, TupleType.PRODUCT, namedElements, metas)
        }
    }
}

fun parseNamedElements(elementSexps: List<IonElement>): List<NamedElement> =
    elementSexps.asSequence()
        .map { it.sexpValue }
        .map { elementSexp ->
            if(elementSexp.size != 2) {
                parseError(elementSexp, ParserErrorContext.InvalidArity(2, elementSexp.size))
            }
            val elementName = elementSexp[0].symbolValue
            val typeRef = parseSingleTypeRef(elementSexp[1])
            NamedElement(elementName, typeRef, elementSexp.metas)
        }
        .toList()



private fun parseSum(sexp: IonElementContainer): DataType.Sum {
    val args = sexp.tail // Skip tag
    val typeName = args.head.symbolValue

    val variants = args.tail.map {
        parseSumVariant(it.sexpValue)
    }

    return DataType.Sum(typeName, variants.toList(), sexp.metas)
}

private fun parseSumVariant(sexp: IonElementContainer): DataType.Tuple {
    return parseTupleBody(sexp, sexp.metas)
}

private fun parseSingleTypeRef(typeRefValue: IonElement): TypeRef {
    val metas = typeRefValue.metas
    return when (typeRefValue.type) {
        IonType.SYMBOL -> TypeRef(typeRefValue.symbolValue, Arity.Required, metas)
        IonType.SEXP -> {
            val typeRefExp = typeRefValue.sexpValue
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
        else -> parseError(typeRefValue, ParserErrorContext.ExpectedSymbolOrSexp(typeRefValue.type))
    }
}

private fun parsePermuteDomain(domainName: String, sexp: IonElementContainer): PermutedDomain {
    val args = sexp.tail // Skip tag

    val permutingDomain = args.head.symbolValue
    val removedTypes = mutableListOf<String>()
    val newTypes = mutableListOf<DataType>()
    val permutedSums = mutableListOf<PermutedSum>()

    val alterSexps = args.tail
    alterSexps.map { it.sexpValue }.forEach { alterSexp ->
        when(alterSexp.head.symbolValue) {
            "with" -> permutedSums.add(parseWithSum(alterSexp))
            "exclude" -> alterSexp.tail.mapTo(removedTypes) { it.symbolValue }
            "include" -> alterSexp.tail.mapTo(newTypes) { parseDomainLevelStatement(it.sexpValue) }
            else -> parseError(alterSexp, ParserErrorContext.InvalidPermutedDomainTag(alterSexp.head.symbolValue))
        }
    }

    return PermutedDomain(
        name = domainName,
        permutesDomain = permutingDomain,
        excludedTypes = removedTypes,
        includedTypes = newTypes,
        permutedSums = permutedSums,
        metas = sexp.metas)
}

private fun parseWithSum(sexp: IonElementContainer): PermutedSum {
    val args = sexp.tail // Skip tag

    val nameOfAlteredSum = args.head.symbolValue
    val removedVariants = mutableListOf<String>()
    val addedVariants = mutableListOf<DataType.Tuple>()

    args.tail.forEach { alterationValue ->
        val alterationSexp = alterationValue.sexpValue
        when (val alterationTag = alterationSexp.tag) {
            "exclude" -> alterationSexp.tail.mapTo(removedVariants) { it.symbolValue }
            "include" -> alterationSexp.tail.mapTo(addedVariants) { parseSumVariant(it.sexpValue) }
            else -> parseError(alterationSexp, ParserErrorContext.InvalidWithSumTag(alterationTag))
        }
    }

    return PermutedSum(nameOfAlteredSum, removedVariants, addedVariants, sexp.metas)
}

private fun requireArityForTag(sexp: IonElementContainer, arity: Int) {
    // Note: arity does not include the tag!
    val argCount = sexp.count() - 1
    if(argCount != arity) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(IntRange(arity, arity), sexp.head.symbolValue, argCount))
    }
}

private fun requireArityForTag(sexp: IonElementContainer, arityRange: IntRange) {
    // Note: arity does not include the tag!
    val argCount = sexp.count() - 1
    if(argCount !in arityRange) {
        parseError(sexp, ParserErrorContext.InvalidArityForTag(arityRange, sexp.head.symbolValue, argCount))
    }
}

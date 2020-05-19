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

package org.partiql.pig.domain.model

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.locationToString


/** Base class for top level statements of a type universe definition. */ 
sealed class Statement(val metas: MetaContainer)

/** Represents a fully defined type domain. */
class TypeDomain(
    /** The name of the type domain. */
    val name: String,
    /** The list of user-defined types.  Does not include primitive types. */
    val userTypes: List<DataType>,
    metas: MetaContainer = emptyMetaContainer()
): Statement(metas) {

    /** All data types. (User types + primitives). */
    val types: List<DataType> = listOf(DataType.Int, DataType.Symbol, DataType.Ion) + userTypes

    fun resolveTypeRef(typeRef: TypeRef) =
        /**
         * This is not call `invalidDomain` because we're assuming that the domain has been checked for errors
         * If an [InvalidStateException] is thrown here a bug probably exists in [TypeDomain]'s error checking.
         */
        types.find { it.tag == typeRef.typeName }
            ?: error("${locationToString(typeRef.metas.location)}: Couldn't resolve type '${typeRef.typeName}'")
}

/**
 * Represents differences to another type domain expressed as deltas.
 *
 * Given a [TypeDomain] and a [PermutedDomain] a new [TypeDomain] can be computed which differs from the original
 * as specified by the [PermutedDomain].
 */
class PermutedDomain(
    val name: String,
    val permutesDomain: String,
    val excludedTypes: List<String>,
    val includedTypes: List<DataType>,
    val permutedSums: List<PermutedSum>,
    metas: MetaContainer
) : Statement(metas) {

    /**
     * Given a map of concrete type domains keyed by name, generates a new concrete type domain with the deltas
     * of this [PermutedDomain] instance applied.
     */
    fun computePermutation(domains: Map<String, TypeDomain>): TypeDomain {
        val permutingDomain =
            domains[this.permutesDomain]
            ?: semanticError(metas, SemanticErrorContext.DomainPermutesNonExistentDomain(name, permutesDomain))

        val newTypes = permutingDomain.types.toMutableList()

        excludedTypes.forEach { removedTypeName ->
            val typeToRemove = newTypes.singleOrNull { it.tag == removedTypeName }

            when {
                typeToRemove == null -> {
                   semanticError(
                       metas,
                       SemanticErrorContext.CannotRemoveNonExistentType(removedTypeName, name, permutesDomain))
                }
                typeToRemove.isBuiltin -> {
                    semanticError(this.metas, SemanticErrorContext.CannotRemoveBuiltinType(removedTypeName))
                }
                else -> {
                    if(!newTypes.removeIf { oldType -> oldType.tag == removedTypeName }) {
                        error("Failed to remove $removedTypeName for some reason")
                    }
                }
            }
        }

        // We do alterations first since if we alter a new type and then add another with the same name
        // it will cause a duplicate type name error.  This would not happen in the reverse order.
        permutedSums.forEach { extSum ->
            when(val typeToAlter = newTypes.singleOrNull { it.tag == extSum.tag }) {
                null -> {
                    semanticError(
                        extSum.metas,
                        SemanticErrorContext.CannotPermuteNonExistentSum(extSum.tag, name, permutesDomain))
                }
                is DataType.Tuple, is DataType.Int, is DataType.Symbol -> {
                    semanticError(extSum.metas, SemanticErrorContext.CannotPermuteNonSumType(extSum.tag))
                }
                is DataType.Sum -> {
                    val newVariants = typeToAlter.variants.toMutableList()

                    val removedVariantTags = extSum.removedVariants.toSet()
                    removedVariantTags.forEach { removedTagName ->
                        if(!newVariants.removeIf { it.tag == removedTagName}) {
                            semanticError(
                                extSum.metas,
                                SemanticErrorContext.CannotRemoveNonExistentSumVariant(extSum.tag, removedTagName))
                        }
                    }

                    newVariants.addAll(extSum.addedVariants)
                    val newSumType = DataType.Sum(extSum.tag, newVariants, metas)

                    if(!newTypes.remove(typeToAlter))
                        // If this happens it's a bug
                        error("Failed to remove altered type '${typeToAlter.tag}' for some reason")

                    newTypes.add(newSumType)
                }
            }
        }

        newTypes.addAll(this.includedTypes)

        // errorCheck is being called by TypeUniverse.resolveExtensions
        return TypeDomain(name, newTypes.filter { !it.isBuiltin }, metas)
    }
}


/** Represents differences to a sum in the domain being permuted. */
class PermutedSum(
    val tag: String,
    val removedVariants: List<String>,
    val addedVariants: List<DataType.Tuple>,
    val metas: MetaContainer
)

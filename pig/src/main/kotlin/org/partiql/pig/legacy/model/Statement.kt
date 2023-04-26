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

package org.partiql.pig.legacy.model

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.locationToString

/** Base class for top level statements of a type universe definition. */
sealed class Statement {
    abstract val metas: MetaContainer
}

/**
 * Represents a fully defined type domain.
 *
 * May also represent the difference between two domains.  See [computeTransform] for details.
 */
class TypeDomain(
    /** The name of the type domain. */
    val tag: String,
    /** The list of user-defined types.  Does not include primitive types. */
    val userTypes: List<DataType.UserType>,
    override val metas: MetaContainer = emptyMetaContainer()
) : Statement() {

    /** All data types. (User types + primitives). */
    val types: List<DataType> = listOf(DataType.Bool, DataType.Int, DataType.Symbol, DataType.Ion) + userTypes

    fun resolveTypeRef(typeRef: TypeRef) =
        /**
         * This is not call `invalidDomain` because we're assuming that the domain has been checked for errors
         * If an [IllegalStateException] is thrown here a bug probably exists in [TypeDomain]'s error checking.
         */
        types.find { it.tag == typeRef.typeName }
            ?: error("${locationToString(typeRef.metas.location)}: Couldn't resolve type '${typeRef.typeName}'")

    /**
     * Creates a copy of this [TypeDomain] while setting the [DataType.UserType.isDifferent] on any user
     * data types that are present in the current domain but not in [destination].
     *
     * For domain-level products and sum variants that do exist in [destination] and have different
     * definitions, [DataType.UserType.isDifferent] is also set.
     *
     * The resulting [TypeDomain] can later be used by the language targets to generate visitors and the like
     * which can transform from one type domain to another, while still generating transform methods for all common
     * data types.
     */
    fun computeTransform(destination: TypeDomain): TypeDomain {
        val destUserTypes = destination.userTypes

        val transformedTypes = userTypes
            .mapNotNull { srcType ->
                val destType = destUserTypes.firstOrNull { it.tag == srcType.tag }

                when {
                    // original type was not found in destination domain... it was removed, don't include it.
                    destType == null -> null
                    srcType is DataType.UserType.Tuple && destType is DataType.UserType.Tuple ->
                        when (srcType) {
                            // sourceType is unmodified from the original
                            destType -> srcType
                            else -> srcType.copyAsDifferent()
                        }
                    srcType is DataType.UserType.Sum && destType is DataType.UserType.Sum -> {
                        // both types are sums, now lets check to see what variants exist
                        val newVariants = srcType.variants.map { srcVariant ->
                            val destVariant = destType.variants.firstOrNull { it.tag == srcVariant.tag }
                            when {
                                destVariant == null || srcVariant != destVariant -> srcVariant.copyAsDifferent()
                                else -> srcVariant
                            }
                        }
                        srcType.copy(variants = newVariants)
                    }
                    // One type is sum and one is variant, mark as removed.
                    else -> destType.copyAsDifferent()
                }
            }

        return TypeDomain(
            tag = tag,
            userTypes = transformedTypes,
            metas = this.metas
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeDomain) return false

        if (tag != other.tag) return false
        if (userTypes != other.userTypes) return false
        return types == other.types
        // Metas intentionally omitted here
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + userTypes.hashCode()
        result = 31 * result + types.hashCode()
        // Metas intentionally omitted here

        return result
    }
}

/**
 * Represents differences to another type domain expressed as deltas.
 *
 * Given a [TypeDomain] and a [PermutedDomain] a new [TypeDomain] can be computed which differs from the original
 * as specified by the [PermutedDomain].
 */
data class PermutedDomain(
    val tag: String,
    val permutesDomain: String,
    val excludedTypes: List<String>,
    val includedTypes: List<DataType.UserType>,
    val permutedSums: List<PermutedSum>,
    override val metas: MetaContainer
) : Statement() {

    /**
     * Given a map of concrete type domains keyed by name, generates a new concrete type domain with the deltas
     * of this [PermutedDomain] instance applied.
     */
    fun computePermutation(domains: Map<String, TypeDomain>): TypeDomain {
        val permutingDomain =
            domains[this.permutesDomain]
                ?: semanticError(metas, SemanticErrorContext.DomainPermutesNonExistentDomain(tag, permutesDomain))

        val newTypes = permutingDomain.types.toMutableList()

        excludedTypes.forEach { removedTypeName ->
            val typeToRemove = newTypes.singleOrNull { it.tag == removedTypeName }

            when {
                typeToRemove == null -> {
                    semanticError(
                        metas,
                        SemanticErrorContext.CannotRemoveNonExistentType(removedTypeName, tag, permutesDomain)
                    )
                }
                typeToRemove.isBuiltin -> {
                    semanticError(this.metas, SemanticErrorContext.CannotRemoveBuiltinType(removedTypeName))
                }
                else -> {
                    if (!newTypes.removeIf { oldType -> oldType.tag == removedTypeName }) {
                        error("Failed to remove $removedTypeName for some reason")
                    }
                }
            }
        }

        // We do alterations first since if we alter a new type and then add another with the same name
        // it will cause a duplicate type name error.  This would not happen in the reverse order.
        permutedSums.forEach { extSum ->
            when (val typeToAlter = newTypes.singleOrNull { it.tag == extSum.tag }) {
                null -> {
                    semanticError(
                        extSum.metas,
                        SemanticErrorContext.CannotPermuteNonExistentSum(extSum.tag, tag, permutesDomain)
                    )
                }
                is DataType.UserType.Tuple, is DataType.Int, is DataType.Symbol -> {
                    semanticError(extSum.metas, SemanticErrorContext.CannotPermuteNonSumType(extSum.tag))
                }
                is DataType.UserType.Sum -> {
                    val newVariants = typeToAlter.variants.toMutableList()

                    val removedVariantTags = extSum.removedVariants.toSet()
                    removedVariantTags.forEach { removedTagName ->
                        if (!newVariants.removeIf { it.tag == removedTagName }) {
                            semanticError(
                                extSum.metas,
                                SemanticErrorContext.CannotRemoveNonExistentSumVariant(extSum.tag, removedTagName)
                            )
                        }
                    }

                    newVariants.addAll(extSum.addedVariants)
                    val newSumType = DataType.UserType.Sum(
                        tag = extSum.tag,
                        variants = newVariants,
                        metas = metas
                    )

                    if (!newTypes.remove(typeToAlter)) {
                        // If this happens it's a bug
                        error("Failed to remove altered type '${typeToAlter.tag}' for some reason")
                    }

                    newTypes.add(newSumType)
                }
                else -> {}
            }
        }

        newTypes.addAll(this.includedTypes)

        // errorCheck is being called by TypeUniverse.resolveExtensions
        return TypeDomain(tag, newTypes.filterIsInstance<DataType.UserType>(), metas)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PermutedDomain) return false

        if (tag != other.tag) return false
        if (permutesDomain != other.permutesDomain) return false
        if (excludedTypes != other.excludedTypes) return false
        if (includedTypes != other.includedTypes) return false
        return permutedSums == other.permutedSums
        // Metas intentionally omitted here
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + permutesDomain.hashCode()
        result = 31 * result + excludedTypes.hashCode()
        result = 31 * result + includedTypes.hashCode()
        result = 31 * result + permutedSums.hashCode()
        // Metas intentionally omitted here

        return result
    }
}

/** Represents differences to a sum in the domain being permuted. */
data class PermutedSum(
    val tag: String,
    val removedVariants: List<String>,
    val addedVariants: List<DataType.UserType.Tuple>,
    val metas: MetaContainer
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PermutedSum) return false

        if (tag != other.tag) return false
        if (removedVariants != other.removedVariants) return false
        return addedVariants == other.addedVariants
        // Metas intentionally omitted here
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + removedVariants.hashCode()
        result = 31 * result + addedVariants.hashCode()
        // Metas intentionally omitted here

        return result
    }
}

/**
 * Transforms will generate target specific code to aid in the transformation of
 * one type domain to another.
 */
data class Transform(
    val sourceDomainTag: String,
    val destinationDomainTag: String,
    override val metas: MetaContainer
) : Statement() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transform) return false

        if (sourceDomainTag != other.sourceDomainTag) return false
        return destinationDomainTag == other.destinationDomainTag
    }

    override fun hashCode(): Int {
        var result = sourceDomainTag.hashCode()
        result = 31 * result + destinationDomainTag.hashCode()
        return result
    }
}

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

/**
 * Converts a [TypeDomain] to a [KTypeDomain] which can be passed to the Apache FreeMarker template.
 *
 * Note that this file should not be throwing any exceptions of any kind.  Any error conditions should be
 * detected by [TypeDomain]'s error checking.
 */
package org.partiql.pig.generator.kotlin

import org.partiql.pig.domain.model.*
import org.partiql.pig.util.snakeToCamelCase
import org.partiql.pig.util.snakeToPascalCase


internal fun TypeUniverse.convertToKTypeUniverse(): KTypeUniverse {
    val allTypeDomains: List<TypeDomain> = this.computeTypeDomains()
    val allTransforms = this.statements.filterIsInstance<Transform>()

    val kotlinTypeDomains = allTypeDomains.map { typeDomain ->

        // Find all the transform statements that apply to the current domain
        // TODO:  we are ignoring the name of the transform here...
        val transforms = allTransforms.filter { transform ->
            transform.sourceDomainTag == typeDomain.tag
        }.map { transform ->
            // Fetch the
            allTypeDomains.single { domain -> domain.tag == transform.destinationDomainTag }
        }
        KTypeDomainConverter(typeDomain, transforms, isTransfrom = true).convert()
    }

    return KTypeUniverse(kotlinTypeDomains)
}

private class KTypeDomainConverter(
    private val typeDomain: TypeDomain,
    private val transformTo: List<TypeDomain>,
    private val isTransfrom: Boolean
) {
    private val defaultBaseClass get() = "${typeDomain.tag}Node"

    fun convert(): KTypeDomain {
        val ktTuples = mutableListOf<KTuple>()
        val ktSums = mutableListOf<KSum>()

        typeDomain.types.forEach {
            when(it) {
                DataType.Int, DataType.Symbol, DataType.Ion -> { /* intentionally blank */ }
                is DataType.UserType.Tuple ->
                    ktTuples.add(
                        it.toKProduct(
                            superClass = defaultBaseClass.snakeToPascalCase(),
                            constructorName = it.tag.snakeToPascalCase()))
                is DataType.UserType.Sum ->
                    ktSums.add(
                        KSum(
                            kotlinName = it.tag.snakeToPascalCase(),
                            superClass = defaultBaseClass.snakeToPascalCase(),
                            variants = it.variants.map { v ->
                                v.toKProduct(
                                    superClass = it.tag.snakeToPascalCase(),
                                    constructorName = "${it.tag.snakeToPascalCase()}.${v.tag.snakeToPascalCase()}")
                            },
                            isRemoved = it.isRemoved
                        ))
            }
        }

        val transforms = transformTo.map {
            // TODO:  that empty list is here seems suggests that there might be a better modeling for
            // KTypeDomain and its transforms...
            val converter = KTypeDomainConverter(typeDomain.computeTransform(it), emptyList(), isTransfrom = true)
            KDomainTransform(
                converter.convert(),
                it.tag.snakeToPascalCase()
            )
        }

        return KTypeDomain(
            kotlinName = typeDomain.tag.snakeToPascalCase(),
            tag = typeDomain.tag,
            tuples = ktTuples,
            sums = ktSums,
            transforms = transforms,
            isTransform = isTransfrom
        )
    }

    private fun DataType.UserType.Tuple.toKProduct(superClass: String, constructorName: String): KTuple {
        return KTuple(
            kotlinName = this.tag.snakeToPascalCase(),
            tag = this.tag,
            superClass = superClass,
            constructorName = constructorName,
            properties = computeProperties(this),
            arity = computeArity(),
            builderFunctions = computeBuilderFunctions(tuple = this),
            isRecord = when (this.tupleType) {
                TupleType.PRODUCT -> false
                TupleType.RECORD -> true
            },
            hasVariadicElement = hasVariadicElement(),
            isRemoved = this.isRemoved)
    }

    /**
     * Locates a data type by its tag and returns true if it is a Kotlin primitive.
     */
    private fun isKotlinPrimitive(element: NamedElement) = typeDomain.resolveTypeRef(element.typeReference).isPrimitive

    private fun DataType.UserType.Tuple.hasPrimitiveElement() = this.namedElements.any { isKotlinPrimitive(it) }

    private fun DataType.UserType.Tuple.hasVariadicElement() =
        this.namedElements.any { it.typeReference.arity is Arity.Variadic }

    private fun computeBuilderFunctions(tuple: DataType.UserType.Tuple): List<KBuilderFunction> {
        val hasPrimitiveElement = tuple.hasPrimitiveElement()
        val hasVariadicElement = tuple.hasVariadicElement()

        val functions = mutableListOf<KBuilderFunction>()

        // All tuples will get at least this "uniadic" builder function.
        // This builder function is not suffixed with "_".
        functions.add(computeUniadicBuilderFunction(tuple, useKotlinPrimitives = true))

        // If there are primitive values, then we also need to generate a "uniadic" builder function that
        // accepts the primiitve types, i.e. [LongPrimitive] and [SymbolPrimitive].
        if(hasPrimitiveElement) {
            functions.add(computeUniadicBuilderFunction(tuple, useKotlinPrimitives = false))
        }

        // Same for variadic elements.
        if(hasVariadicElement) {
            functions.add(computeVariadicBuilderFunction(tuple, useKotlinPrimitives = true))
            if(hasPrimitiveElement) {
                functions.add(computeVariadicBuilderFunction(tuple, useKotlinPrimitives = false))
            }
        }

        return functions.toList() // Convert to immutable list
    }

    /** Computes a non-variadic builder function that List<> instead of `vararg` for its variadic element if one exists. */
    private fun computeUniadicBuilderFunction(
        tuple: DataType.UserType.Tuple,
        useKotlinPrimitives: Boolean
    ): KBuilderFunction {

        return KBuilderFunction(
            kotlinName = tuple.tag.snakeToCamelCase() + if(!useKotlinPrimitives) "_" else "",
            parameters = tuple.namedElements
                .map {
                    KParameter(
                        kotlinName =  it.identifier.snakeToCamelCase(),
                        kotlinType = it.typeReference.getQualifiedTypeName(useKotlinPrimitives, useAnyElement = false),
                        defaultValue = if (it.typeReference.arity is Arity.Optional) "null" else null,
                        isVariadic = false)
                },
            constructorArguments = tuple.namedElements.map {
                computeConstructorArgument(it, useKotlinPrimitives = useKotlinPrimitives)
            })
    }

    /** Computes a named argument to a constructor for the given [element]. */
    private fun computeConstructorArgument(
        element: NamedElement,
        useKotlinPrimitives: Boolean
    ): KConstructorArgument {
        val elementKotlinName = element.identifier.snakeToCamelCase()
        val argumentExpr = when {
            isKotlinPrimitive(element) && useKotlinPrimitives ->
                when (element.typeReference.arity) {
                    !is Arity.Variadic -> {
                        val maybeQuestionMark = when (element.typeReference.arity) {
                            is Arity.Optional -> "?"
                            else -> ""
                        }
                        "$elementKotlinName$maybeQuestionMark.asPrimitive()"
                    }
                    else -> "$elementKotlinName.map { it.asPrimitive() }"
                }
            else -> elementKotlinName
        }.let {
            when (element.typeReference.typeName) {
                "ion" -> "$it.asAnyElement()"
                else -> it
            }
        }

        return KConstructorArgument(elementKotlinName, argumentExpr)
    }

    /** Computes a non-variadic builder function that uses a `vararg` argument for its variadic element if one exists. */
    private fun computeVariadicBuilderFunction(tuple: DataType.UserType.Tuple, useKotlinPrimitives: Boolean): KBuilderFunction {
        return KBuilderFunction(
            kotlinName = tuple.tag.snakeToCamelCase() + if(!useKotlinPrimitives) "_" else "",
            parameters = computeExpandedVariadicBuilderFunctionParameters(tuple, useKotlinPrimitives),
            constructorArguments = tuple.namedElements.map { element ->
                val elementIsKotlinPrimitive = isKotlinPrimitive(element)
                val elementKotlinName = element.identifier.snakeToCamelCase()
                when (element.typeReference.arity) {
                    is Arity.Required, Arity.Optional -> {
                        KConstructorArgument(
                            kotlinName = elementKotlinName,
                            value = elementKotlinName + when {
                                elementIsKotlinPrimitive && useKotlinPrimitives -> ".asPrimitive()"
                                else -> ""
                            })
                    }
                    is Arity.Variadic -> {
                        // We're generating the right hand side of the assignment statement here because it's a royal
                        // pain to do this with the FreeMarker template--Kotlin is much better suited to this particular
                        // code-generation task.
                        when {
                            element.typeReference.arity.minimumArity > 0 -> {
                                val requiredParameterList = IntRange(0, element.typeReference.arity.minimumArity - 1)
                                    .joinToString(", ") { paramIndex -> "${elementKotlinName}$paramIndex" }

                                if (elementIsKotlinPrimitive) {
                                    KConstructorArgument(
                                        elementKotlinName,
                                        "listOfPrimitives($requiredParameterList) + $elementKotlinName" + when {
                                            useKotlinPrimitives -> ".map { it.asPrimitive() }"
                                            else -> ".toList()"
                                        })
                                } else {
                                    KConstructorArgument(
                                        elementKotlinName,
                                        "listOf($requiredParameterList) + $elementKotlinName.toList()")
                                }
                            }
                            else -> {
                                KConstructorArgument(
                                    elementKotlinName,
                                    elementKotlinName + when {
                                        elementIsKotlinPrimitive && useKotlinPrimitives -> ".map { it.asPrimitive() }"
                                        else -> ".toList()"
                                    })
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * This computes an complete list of "expanded" variadic parameters for a builder function.
     *
     * "Expansion" here refers adding one argument per required position in a variadic element. For example, the type
     * definition `(product foo::foo bar::int bat::(* symbol 2))` should generate a builder function with 4 total
     * parameters:
     *
     * ```Kotlin
     *     fun foo(
     *        bar: Long,
     *        bat1: String,
     *        bat2: String,
     *        vararg bat: String
     *     ) = ...
     * ```
     *
     * This provides a way of enforcing the minimum arity of variadic fields of at Kotlin compile-time when
     * using a generated builder.
     */
    private fun computeExpandedVariadicBuilderFunctionParameters(tuple: DataType.UserType.Tuple, primitive: Boolean): List<KParameter> =
        tuple.namedElements.map { element ->
            when (val arity = element.typeReference.arity) {
                is Arity.Required, is Arity.Optional -> {
                    listOf(
                        KParameter(
                            kotlinName = element.identifier.snakeToCamelCase(),
                            kotlinType = element.typeReference.getQualifiedTypeName(primitive),
                            defaultValue = if(arity is Arity.Optional) "null" else null,
                            isVariadic = false))
                }
                is Arity.Variadic -> {
                    val requiredParameters =
                        IntRange(0, arity.minimumArity - 1).map { paramIndex ->
                            KParameter(
                                kotlinName = "${element.identifier.snakeToCamelCase()}$paramIndex",
                                kotlinType = element.typeReference.getBaseKotlinTypeName(primitive),
                                defaultValue = null,
                                isVariadic = false)
                        }

                    val variadicParameter =
                        KParameter(
                            kotlinName = element.identifier.snakeToCamelCase(),
                            kotlinType = element.typeReference.getBaseKotlinTypeName(primitive),
                            defaultValue = null,
                            isVariadic = true)

                    requiredParameters + listOf(variadicParameter)
                }
            }
        }.flatten()

    private fun computeTransformExpr(tuple: DataType.UserType.Tuple, element: NamedElement, ordinal: Int): String =
        when(tuple.tupleType) {
            TupleType.RECORD -> {
                val expectCast = createExpectCast(element.typeReference)
                val processFuncName = when (element.typeReference.arity) {
                    is Arity.Required -> "processRequiredField"
                    is Arity.Optional -> "processOptionalField"
                    is Arity.Variadic -> TODO("Variadic elements in records not yet supported")
                }
                "ir.${processFuncName}(\"${element.identifier}\") { it$expectCast }"
            }
            TupleType.PRODUCT -> {
                val expectCast = createExpectCast(element.typeReference)
                when (element.typeReference.arity) {
                    is Arity.Required -> {
                        when (typeDomain.resolveTypeRef(element.typeReference)) {
                            DataType.Ion -> "sexp.getRequiredIon($ordinal)$expectCast"
                            else -> "sexp.getRequired($ordinal)$expectCast"
                        }
                    }
                    is Arity.Optional -> "sexp.getOptional($ordinal)?$expectCast"
                    is Arity.Variadic -> "sexp.values.drop(${ordinal + 1}).map { it$expectCast }"
                }
            }
        }

    private fun computeProperties(tuple: DataType.UserType.Tuple): List<KProperty> =
        tuple.namedElements.mapIndexed { ordinal, element ->
            val deserExpr = computeTransformExpr(tuple, element, ordinal)
            val (isVariadic, isNullable) = when (element.typeReference.arity) {
                is Arity.Required -> false to false
                is Arity.Optional -> false to true
                is Arity.Variadic -> true to false
            }
            KProperty(
                kotlinName = element.identifier.snakeToCamelCase(),
                tag = element.tag,
                kotlinTypeName = element.typeReference.getQualifiedTypeName(useKotlinPrimitives = false),
                isVariadic = isVariadic,
                isNullable = isNullable,
                transformExpr = deserExpr,
                rawTypeName = element.typeReference.rawTypeName)
        }

    private fun createExpectCast(typeRef: TypeRef): String =
        when (typeDomain.resolveTypeRef(typeRef)) {
            DataType.Ion -> ""
            DataType.Int -> ".toLongPrimitive()"
            DataType.Symbol -> ".toSymbolPrimitive()"
            is DataType.UserType.Tuple, is DataType.UserType.Sum ->
                ".transformExpect<${typeRef.typeName.snakeToPascalCase()}>()"
        }

    private fun TypeRef.getQualifiedTypeName(useKotlinPrimitives: Boolean, useAnyElement: Boolean = true): String =
        getBaseKotlinTypeName(useKotlinPrimitives, useAnyElement).let {
            when(this.arity) {
                Arity.Required -> it
                Arity.Optional -> "$it?"
                is Arity.Variadic -> "kotlin.collections.List<$it>"
            }
        }

   private fun TypeRef.getBaseKotlinTypeName(kotlinPrimitives: Boolean, useAnyElement: Boolean = true): String {
        return when (typeName) {
            "ion" -> "com.amazon.ionelement.api." + if(useAnyElement) "AnyElement" else "IonElement"
            "int" -> if (kotlinPrimitives) "Long" else "org.partiql.pig.runtime.LongPrimitive"
            "symbol" -> if (kotlinPrimitives) "String" else "org.partiql.pig.runtime.SymbolPrimitive"
            else -> this.typeName.snakeToPascalCase()
        }
    }


    private val TypeRef.rawTypeName: String
        get() {
            return when (typeName) {
                "ion" -> "AnyElement"
                "int" -> "LongPrimitive"
                "symbol" -> "SymbolPrimitive"
                else -> this.typeName.snakeToPascalCase()
            }
        }
}
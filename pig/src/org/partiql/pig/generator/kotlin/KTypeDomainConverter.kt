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

internal class KTypeDomainConverter(private val typeDomain: TypeDomain) {
    private val defaultBaseClass get() = "${typeDomain.name}_node"

    fun convert(): KTypeDomain {
        val ktProducts = mutableListOf<KTuple>()
        val ktSums = mutableListOf<KSum>()

        typeDomain.types.forEach {
            when(it) {
                DataType.Int, DataType.Symbol, DataType.Ion -> { /* intentionally blank */ }
                is DataType.Tuple ->
                    ktProducts.add(it.toKProduct(superClass = defaultBaseClass, constructorName = it.tag))
                is DataType.Sum ->
                    ktSums.add(
                        KSum(
                            name = it.tag,
                            superClass = defaultBaseClass,
                            variants = it.variants.map { v ->
                                v.toKProduct(
                                    superClass = it.tag,
                                    constructorName = "${it.tag}.${v.tag}")
                            }
                        ))
            }
        }

        return KTypeDomain(typeDomain.name, ktProducts, ktSums)
    }

    private fun DataType.Tuple.toKProduct(superClass: String, constructorName: String): KTuple {
        return KTuple(
            name = this.tag,
            superClass = superClass,
            constructorName = constructorName,
            properties = computeProperties(this),
            arity = computeArity(),
            builderFunctions = computeBuilderFunctions(tuple = this),
            isRecord = when (this.tupleType) {
                TupleType.PRODUCT -> false
                TupleType.RECORD -> true
            },
            hasVariadicElement = hasVariadicElement())
    }

    /**
     * Locates a data type by its tag and returns true if it is a Kotlin primitive.
     */
    private fun isKotlinPrimitive(element: NamedElement) = typeDomain.resolveTypeRef(element.typeReference).isPrimitive


    private fun DataType.Tuple.hasPrimitiveElement() = this.namedElements.any { isKotlinPrimitive(it) }

    private fun DataType.Tuple.hasVariadicElement() =
        this.namedElements.any { it.typeReference.arity is Arity.Variadic }

    private fun computeBuilderFunctions(tuple: DataType.Tuple): List<KBuilderFunction> {
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
        tuple: DataType.Tuple,
        useKotlinPrimitives: Boolean
    ): KBuilderFunction {

        return KBuilderFunction(
            name = tuple.tag + if(!useKotlinPrimitives) "_" else "",
            parameters = tuple.namedElements
                .map {
                    KParameter(
                        name =  it.name,
                        type = it.typeReference.toKotlinTypeName(useKotlinPrimitives),
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
        val argumentExpr = when {
            isKotlinPrimitive(element) && useKotlinPrimitives -> {
                if (element.typeReference.arity !is Arity.Variadic) {
                    val maybeQuestionMark = if (element.typeReference.arity is Arity.Optional) "?" else ""
                    element.name + "$maybeQuestionMark.asPrimitive()"
                }
                else {
                    element.name + ".map { it.asPrimitive() }"
                }
            }
            else -> element.name
        }

        return KConstructorArgument(element.name, argumentExpr)
    }

    /** Computes a non-variadic builder function that `vararg` for its variadic element if one exists. */
    private fun computeVariadicBuilderFunction(tuple: DataType.Tuple, useKotlinPrimitives: Boolean): KBuilderFunction {
        return KBuilderFunction(
            name =  tuple.tag + if(!useKotlinPrimitives) "_" else "",
            parameters = computeExpandedVariadicBuilderFunctionParameters(tuple, useKotlinPrimitives),
            constructorArguments = tuple.namedElements.map { element ->
                val elementIsKotlinPrimitive = isKotlinPrimitive(element)
                when (element.typeReference.arity) {
                    is Arity.Required, Arity.Optional ->
                        KConstructorArgument(
                            element.name,
                            element.name + when {
                                elementIsKotlinPrimitive && useKotlinPrimitives -> ".asPrimitive()"
                                else -> ""
                            })
                    is Arity.Variadic -> {
                        // We're generating the right hand side of the assignment statement here because it's a royal
                        // pain to do this with the FreeMarker template--Kotlin is much better suited to this particular
                        // code-generation task.
                        when {
                            element.typeReference.arity.minimumArity > 0 -> {
                                val requiredParameterList = IntRange(0, element.typeReference.arity.minimumArity - 1)
                                    .joinToString(", ") { paramIndex -> "${element.name}_required_$paramIndex" }

                                if (elementIsKotlinPrimitive) {
                                    KConstructorArgument(
                                        element.name,
                                        "listOfPrimitives($requiredParameterList) + ${element.name}" + when {
                                            useKotlinPrimitives -> ".map { it.asPrimitive() }"
                                            else -> ".toList()"
                                        })
                                } else {
                                    KConstructorArgument(
                                        element.name,
                                        "listOf($requiredParameterList) + ${element.name}.toList()")
                                }
                            }
                            else -> {
                                KConstructorArgument(
                                    element.name,
                                    element.name + when {
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
     * definition `(product foo int (* symbol 2))` should generate a builder function with 4 total parameters:
     *
     * ```Kotlin
     *     fun foo(
     *        int0: Long,
     *        symbol1_required_0: String,
     *        symbol1_required_1: String,
     *        vararg symbol1: String
     *     ) = ...
     * ```
     *
     * This provides a way of enforcing the minimum arity of variadic fields of at Kotlin compile-time when
     * using a generated builder.
     */
    private fun computeExpandedVariadicBuilderFunctionParameters(tuple: DataType.Tuple, primitive: Boolean): List<KParameter> =
        tuple.namedElements.map { element ->
            when (val arity = element.typeReference.arity) {
                is Arity.Required, is Arity.Optional -> {
                    listOf(
                        KParameter(
                            name = element.name,
                            type = element.typeReference.toKotlinTypeName(primitive),
                            defaultValue = if(arity is Arity.Optional) "null" else null,
                            isVariadic = false))
                }
                is Arity.Variadic -> {
                    val requiredParameters =
                        IntRange(0, arity.minimumArity - 1).map { paramIndex ->
                            KParameter(
                                name = "${element.name}_required_$paramIndex",
                                type = element.typeReference.toBaseKotlinTypeName(primitive),
                                defaultValue = null,
                                isVariadic = false)
                        }

                    val variadicParameter =
                        KParameter(
                            name = element.name,
                            type = element.typeReference.toBaseKotlinTypeName(primitive),
                            defaultValue = null,
                            isVariadic = true)

                    requiredParameters + listOf(variadicParameter)
                }
            }
        }.flatten()

    private fun computeTransformExpr(tuple: DataType.Tuple, element: NamedElement, ordinal: Int): String =
        when(tuple.tupleType) {
            TupleType.RECORD -> {
                val expectCast = createExpectCast(element.typeReference)
                val processFuncName = when (element.typeReference.arity) {
                    is Arity.Required -> "processRequiredField"
                    is Arity.Optional -> "processOptionalField"
                    is Arity.Variadic -> TODO("Variadic elements in records not yet supported")
                }
                "ir.${processFuncName}(\"${element.name}\") { it$expectCast }"
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

    private fun computeProperties(tuple: DataType.Tuple): List<KProperty> {
        val properties = tuple.namedElements.mapIndexed { ordinal, element ->
            val deserExpr = computeTransformExpr(tuple, element, ordinal)
            val (isVariadic, isNullable) = when (element.typeReference.arity) {
                is Arity.Required -> false to false
                is Arity.Optional -> false to true
                is Arity.Variadic -> true to false
            }
            KProperty(
                name = element.name,
                type = element.typeReference.toKotlinTypeName(useKotlinPrimitives = false),
                isVariadic = isVariadic,
                isNullable = isNullable,
                transformExpr = deserExpr)
        }
        return properties
    }

    private fun createExpectCast(typeRef: TypeRef): String {
        val dataType = typeDomain.resolveTypeRef(typeRef)
        val expectExpr = when (dataType) {
            DataType.Ion -> ""
            DataType.Int -> ".toLongPrimitive()"
            DataType.Symbol -> ".toSymbolPrimitive()"
            is DataType.Tuple, is DataType.Sum ->
                ".transformExpect<${typeRef.typeName.quotedKotlinKeyword}>()"
        }
        return expectExpr
    }

    fun TypeRef.toKotlinTypeName(useKotlinPrimitives: Boolean): String =
        toBaseKotlinTypeName(useKotlinPrimitives).let {
            when(this.arity) {
                Arity.Required -> it
                Arity.Optional -> "$it?"
                is Arity.Variadic -> "List<$it>"
            }
        }

    private fun TypeRef.toBaseKotlinTypeName(kotlinPrimitives: Boolean): String {

        return when (typeName) {
            "ion" -> "IonElement"
            "int" -> if (kotlinPrimitives) "Long" else "LongPrimitive"
            "symbol" -> if (kotlinPrimitives) "String" else "SymbolPrimitive"
            else -> this.typeName.quotedKotlinKeyword
        }
    }

    private val String.quotedKotlinKeyword
        get() = if (KOTLIN_KEYWORDS.contains(this)) {
            "`$this`"
        } else {
            this
        }

    // TODO:  expand on this https://kotlinlang.org/docs/reference/keyword-reference.html
    private val KOTLIN_KEYWORDS = setOf(
        "as",
        "in",
        "is"
    )
}
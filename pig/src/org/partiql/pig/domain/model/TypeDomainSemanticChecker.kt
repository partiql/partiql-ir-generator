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

/**
 * Performs semantic checking for [TypeDomain].
 */
fun TypeDomain.checkSemantics() = TypeDomainSemanticChecker(this).checkSemantics()

/**
 * Performs semantic checking for [TypeDomain].
 *
 * Single use per instance.  Discard after each instance after invoking [checkSemantics].
 * Instances may not be shared among threads.
 */
private class TypeDomainSemanticChecker(private val typeDomain: TypeDomain) {

    private enum class NameType {
        TYPE,
        VARIANT,
        NAMED_ELEMENT
    }

    private val names = mutableMapOf<String, NameType>()

    fun checkSemantics() {
        collectNames()
        errorCheck()
    }

    /**
     * Makes a pass over all types defined in the type domain and populates [names] with all the names data type
     * names, variant names and field names defined within.
     *
     * Along the way, we are also checking to ensure that names aren't being re-used, etc.
     */
    private fun collectNames() {
        typeDomain.types.forEach { dataType ->
            // Check that the data type name isn't already used.
            if(this.names.putIfAbsent(dataType.tag, NameType.TYPE) != null) {
                semanticError(dataType.metas, SemanticErrorContext.NameAlreadyUsed(dataType.tag, typeDomain.name))
            }

            when (dataType) {
                is DataType.Tuple -> { /* no validation needed */
                    when (dataType.tupleType) {
                        TupleType.PRODUCT -> { /* ok, we allow this */}
                        TupleType.RECORD -> collectRecordNames(dataType)
                    }
                }
                is DataType.Sum -> {
                    dataType.variants.forEach { variant ->
                        // Check that the variant name isn't already used.
                        if (names.putIfAbsent(variant.tag, NameType.VARIANT) != null) {
                            semanticError(variant.metas, SemanticErrorContext.NameAlreadyUsed(variant.tag, typeDomain.name))
                        }

                        when(variant.tupleType) {
                            TupleType.PRODUCT -> { /* products to not define additional names */ }
                            TupleType.RECORD ->  collectRecordNames(variant)
                        }
                    }
                }
                DataType.Ion,
                DataType.Int,
                DataType.Symbol -> {
                    /* do nothing, these are always valid */
                }
            }
        }
    }

    private fun collectRecordNames(variant: DataType.Tuple) {
        val elementNames = mutableSetOf<String>()
        variant.namedElements.forEach {
            // Check that the element name hasn't already been used.
            when {
                !elementNames.contains(it.name) -> elementNames.add(it.name)
                else -> semanticError(it.metas, SemanticErrorContext.DuplicateRecordElementName(it.name))
            }
        }
    }

    /**
     * Makes a pass over all types defined in the type domain and checks for semantic errors.  Assumes
     * [collectNames] has already been called and that [names] has been populated as a result.
     */
    private fun errorCheck() {
        typeDomain.types.forEach { dataType ->
            when (dataType) {
                is DataType.Tuple -> {
                    checkTupleForErrors(dataType)
                }
                is DataType.Sum -> {
                    if (dataType.variants.none()) {
                        semanticError(dataType.metas, SemanticErrorContext.EmptySumType(dataType.tag))
                    }
                    dataType.variants.forEach { variant ->
                        checkTupleForErrors(variant)
                    }
                }
                DataType.Ion,
                DataType.Int,
                DataType.Symbol -> {
                    /* do nothing, these are always valid */
                }
            }
        }
    }

    private fun checkTupleForErrors(t: DataType.Tuple) {
        when(t.tupleType) {
            TupleType.PRODUCT -> checkProductForErrors(t)
            TupleType.RECORD -> checkRecordForErrors(t)
        }
    }

    // Check all type references to ensure they refer to valid types
    private fun checkProductForErrors(p: DataType.Tuple) {
        p.namedElements.forEach { typeRef ->
            checkTypeRef(typeRef.typeReference)
        }

        checkProductIonFieldArity(p)
        checkProductArgumentOrder(p)
    }

    private fun checkRecordForErrors(r: DataType.Tuple) {
        r.namedElements.forEach {
            checkTypeRef(it.typeReference)
        }
    }

    private fun checkTypeRef(typeRef: TypeRef) {
        when (this.names[typeRef.typeName]) {
            null -> {
                semanticError(typeRef.metas, SemanticErrorContext.UndefinedType(typeRef.typeName))
            }
            NameType.VARIANT, NameType.NAMED_ELEMENT -> {
                semanticError(typeRef.metas, SemanticErrorContext.NotATypeName(typeRef.typeName))
            }
            NameType.TYPE -> { /* [et] refers to a type!  this is the success case! */ }
        }
    }


    private enum class ArgumentState {
        REQUIRED,
        OPTIONAL,
        VARIADIC
    }

    /**
     * Validate the ordering of argument arities.  Rules are:
     *
     * - A product may have zero or more required arguments followed by only one of the following:
     * - One or more optional arguments  - A single variadic argument.
     *
     *  This is implemented as a simple state machine.
     */

    private fun checkProductArgumentOrder(p: DataType.Tuple) {
        var currentState = ArgumentState.REQUIRED
        p.namedElements.forEach { element ->
            val arity = element.typeReference.arity
            when (currentState) {
                ArgumentState.REQUIRED -> {
                    when (arity) {
                        Arity.Required -> { /* do nothing, it's all good */
                        }
                        is Arity.Optional -> {
                            currentState = ArgumentState.OPTIONAL
                        }
                        is Arity.Variadic -> {
                            currentState = ArgumentState.VARIADIC
                        }
                    }
                }
                ArgumentState.OPTIONAL -> {
                    when (arity) {
                        Arity.Required -> {
                            semanticError(element.metas, SemanticErrorContext.RequiredElementAfterOptional)
                        }
                        is Arity.Optional -> {/* do nothing, it's all good */
                        }
                        is Arity.Variadic -> {
                            semanticError(element.metas, SemanticErrorContext.ProductCannotHaveBothOptionalAndVariadicElements)
                        }
                    }
                }
                ArgumentState.VARIADIC -> {
                    when (arity) {
                        Arity.Required -> {
                            semanticError(element.metas, SemanticErrorContext.RequiredElementAfterVariadic)
                        }
                        is Arity.Optional -> {
                            semanticError(element.metas, SemanticErrorContext.OptionalElementAfterVariadic)
                        }
                        is Arity.Variadic -> {
                            semanticError(element.metas, SemanticErrorContext.MoreThanOneVariadicElement)
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate there's no optional [DataType.Ion] argument.
     *
     * We don't support such argument because it causes confusion but doesn't
     * add much user value. A required [DataType.Ion] argument accepts same
     * value set as an optional one, while the semantic of IonNull is clearer
     * there: It's a valid value of this argument, not a placeholder for
     * unprovided optional argument.
     */
    private fun checkProductIonFieldArity(p: DataType.Tuple) =
        p.namedElements.forEach { element ->
            val dataType = typeDomain.resolveTypeRef(element.typeReference)
            if (dataType == DataType.Ion && element.typeReference.arity == Arity.Optional)
                semanticError(element.metas, SemanticErrorContext.OptionalIonTypeElement)
        }
}

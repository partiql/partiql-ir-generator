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

import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.location
import org.partiql.pig.errors.PigException
import org.partiql.pig.errors.ErrorContext
import org.partiql.pig.errors.PigError

/**
 * Encapsulates all error context information in an easily testable way.
 *
 * TODO:  use one class derived from [SemanticErrorContext] per location where an error may be thrown? This makes
 *        being sure that every error condition is tested easier but complicates [SemanticErrorContext] due
 *        to the increased number of derived classes. (Currently [NameAlreadyUsed] is used at several places.)
 */
sealed class SemanticErrorContext(val msgFormatter: () -> String): ErrorContext {
    override val message: String get() = msgFormatter()

    object EmptyUniverse
        : SemanticErrorContext({ "The universe was empty" })

    data class UndefinedType(val typeName: String)
        : SemanticErrorContext({ "Undefined type: '$typeName'"})

    data class NotATypeName(val variantName: String)
        : SemanticErrorContext({ "Cannot use a variant name '$variantName' here"})

    object RequiredElementAfterOptional
        : SemanticErrorContext({ "Required fields not allowed after optional fields"})

    object ProductCannotHaveBothOptionalAndVariadicElements
        : SemanticErrorContext({ "A product cannot have both optional and variadic fields"})

    object RequiredElementAfterVariadic
        : SemanticErrorContext({ "Required fields are not allowed after a variadic field" })

    object OptionalElementAfterVariadic
        : SemanticErrorContext({ "Optional fields are not allowed after a variadic field"})

    object MoreThanOneVariadicElement
        : SemanticErrorContext({ "A product may not have more than one variadic field" })

    object OptionalIonTypeElement
        : SemanticErrorContext({ "A product may not have optional Ion type field" })

    object EmptyRecord
        : SemanticErrorContext({ "Records must have at least one field" })

    data class CannotRemoveBuiltinType(val typeName: String)
        : SemanticErrorContext({ "Cannot remove built-in type '$typeName'" })

    data class DuplicateTypeDomainName(val domainName: String)
        : SemanticErrorContext({ "Duplicate type domain tag: '${domainName} "})

    data class DuplicateRecordElementTag(val elementName: String)
        : SemanticErrorContext({ "Duplicate record element tag: '${elementName} "})

    data class DuplicateElementIdentifier(val elementName: String)
        : SemanticErrorContext({ "Duplicate element identifier: '${elementName} "})

    data class NameAlreadyUsed(val name: String, val domainName: String)
        : SemanticErrorContext({ "Name '$name' was previously used in the `$domainName` type domain" })

    data class CannotRemoveNonExistentSumVariant(val sumTypeName: String, val variantName: String)
        : SemanticErrorContext({ "Permuted sum type '${sumTypeName}' tries to remove variant '${variantName}' which " +
                               "does not exist in the original sum type" })

    data class DomainPermutesNonExistentDomain(val domainName: String, val permutedDomain: String)
        : SemanticErrorContext({ "Domain '$domainName' permutes non-existent domain '$permutedDomain'"})

    data class CannotRemoveNonExistentType(val typeName: String, val permutingDomain: String, val permuteeDomain: String)
        : SemanticErrorContext({ "Domain '$permutingDomain' tries to remove type '$typeName', which does not exist in the " +
                               "domain being permuted: '$permuteeDomain'." })

    data class CannotPermuteNonExistentSum(val typeName: String, val permutingDomain: String, val permuteeDomain: String)
        : SemanticErrorContext({ "Domain '$permutingDomain' tries to permute type '$typeName', which does not exist in the " +
                               "domain being permuted: '$permuteeDomain'." })

    data class CannotPermuteNonSumType(val typeName: String)
        : SemanticErrorContext({ "Cannot permute type '$typeName' because it is not a sum" })

    data class EmptySumType(val sumTypeName: String)
        : SemanticErrorContext({ "Sum type '$sumTypeName' is empty" })

    data class SourceDomainDoesNotExist(val name: String)
        : SemanticErrorContext({ "Source domain '$name' does not exist" })

    data class DestinationDomainDoesNotExist(val name: String)
        : SemanticErrorContext({ "Destination domain '$name' does not exist" })
}

/**
 * Shortcut for throwing [PigException] with the specified metas and [PigError].
 */
fun semanticError(blame: MetaContainer, context: ErrorContext): Nothing =
    semanticError(blame.location, context)
/**
 * Shortcut for throwing [PigException] with the specified metas and [PigError].
 */
fun semanticError(blame: IonLocation?, context: ErrorContext): Nothing =
    throw PigException(PigError(blame, context))

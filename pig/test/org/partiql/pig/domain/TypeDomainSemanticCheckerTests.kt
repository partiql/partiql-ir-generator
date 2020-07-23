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

package org.partiql.pig.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.domain.model.SemanticErrorContext
import org.partiql.pig.domain.parser.parseTypeUniverse
import org.partiql.pig.errors.PigError
import org.partiql.pig.errors.PigException

class TypeDomainSemanticCheckerTests {

    @ParameterizedTest
    @MethodSource("parametersForMiscErrorsTest")
    fun miscErrorsTest(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForPerumteErrorsTest")
    fun perumteErrorsTest(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNameErrorsTest1")
    fun nameErrorsTest1(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("parametersForNameErrorsTest2")
    fun nameErrorsTest2(tc: TestCase) = runTest(tc)

    private fun runTest(tc: TestCase) {
        val u = parseTypeUniverse(tc.typeUniverseText)
        val ex = assertThrows<PigException> { u.computeTypeDomains() }
        assertEquals(tc.expectedError, ex.error)
    }

    companion object {
        data class TestCase(val typeUniverseText: String, val expectedError: PigError)

        @JvmStatic
        @Suppress("unused")
        fun parametersForMiscErrorsTest() = listOf(
            TestCase("", makeErr(SemanticErrorContext.EmptyUniverse)),

            TestCase("(define foo (domain)) (define foo (domain))",
                     makeErr(1, 35, SemanticErrorContext.DuplicateTypeDomainName("foo"))),

            // Sum types should not be empty
            TestCase("(define some_domain (domain (sum empty_sum)))",
                     makeErr(1, 29, SemanticErrorContext.EmptySumType("empty_sum"))),

            // Product element arity ordering
            TestCase("(define some_domain (domain (product some_product x::(? int) y::int)))",
                     makeErr(1, 62, SemanticErrorContext.RequiredElementAfterOptional)),

            TestCase("(define some_domain (domain (product some_product x::(? int) y::(* int 1))))",
                     makeErr(1, 62, SemanticErrorContext.ProductCannotHaveBothOptionalAndVariadicElements)),

            TestCase("(define some_domain (domain (product some_product x::(* int 1) y::int)))",
                     makeErr(1, 64, SemanticErrorContext.RequiredElementAfterVariadic)),

            TestCase("(define some_domain (domain (product some_product x::(* int 1) y::(? int))))",
                     makeErr(1, 64, SemanticErrorContext.OptionalElementAfterVariadic)),

            TestCase("(define some_domain (domain (product some_product x::(* int 1) y::(* int 1))))",
                     makeErr(1, 64, SemanticErrorContext.MoreThanOneVariadicElement)),

            TestCase("(define some_domain (domain (product some_product x::(? ion))))",
                     makeErr(1, 51, SemanticErrorContext.OptionalIonTypeElement)))

        @JvmStatic
        @Suppress("unused")
        fun parametersForPerumteErrorsTest() = listOf(
            TestCase("(define foo (permute_domain nonexistent))",
                     makeErr(1, 13, SemanticErrorContext.DomainPermutesNonExistentDomain("foo", "nonexistent"))),

            TestCase("(define some_domain (domain)) (define some_permuted_domain (permute_domain some_domain (exclude nonexistent)))",
                     makeErr(1, 60, SemanticErrorContext.CannotRemoveNonExistentType("nonexistent", "some_permuted_domain", "some_domain"))),

            TestCase("(define some_domain (domain)) (define some_permuted_domain (permute_domain some_domain (with nonexistent)))",
                     makeErr(1, 88, SemanticErrorContext.CannotPermuteNonExistentSum("nonexistent", "some_permuted_domain", "some_domain"))),

            TestCase("(define some_domain (domain (sum some_sum (a x::int)))) (define some_permuted_domain (permute_domain some_domain (with some_sum (exclude nonexistent))))",
                     makeErr(1, 114, SemanticErrorContext.CannotRemoveNonExistentSumVariant(sumTypeName = "some_sum", variantName = "nonexistent"))),

            TestCase("(define some_domain (domain)) (define some_domain (permute_domain some_domain (with int)))",
                     makeErr(1, 79, SemanticErrorContext.CannotPermuteNonSumType("int"))),

            TestCase("(define some_domain (domain (product x::foo y::int))) (define some_domain (permute_domain some_domain (with foo)))",
                     makeErr(1, 103, SemanticErrorContext.CannotPermuteNonSumType("foo"))),

            TestCase("(define some_domain (domain)) (define permuted_domain (permute_domain some_domain (exclude int)))",
                     makeErr(1, 55, SemanticErrorContext.CannotRemoveBuiltinType("int"))),

            TestCase("(define some_domain (domain (product some_product x::undefined)))",
                     makeErr(1, 51, SemanticErrorContext.UndefinedType("undefined"))),

            // Excluding the only variant from a sum should result in the EmptySumType error
            TestCase("(define some_domain (domain (sum non_empty_sum (a x::int)))) (define another_domain (permute_domain some_domain (with non_empty_sum (exclude a))))",
                     makeErr(1, 85, SemanticErrorContext.EmptySumType("non_empty_sum"))))

        @JvmStatic
        @Suppress("unused")
        fun parametersForNameErrorsTest1() = listOf(
            // Variant tag used in place of type tag (in same sum)
            TestCase("(define some_domain (domain (sum some_sum (a_variant) (b_variant x::a_variant))))",
                     makeErr(1, 66, SemanticErrorContext.NotATypeName("a_variant"))),

            // Variant tag used in place of type name (in a different sum)
            TestCase("(define some_domain (domain (sum some_sum (a_variant)) (sum another_sum (b_variant x::a_variant))))",
                     makeErr(1, 84, SemanticErrorContext.NotATypeName("a_variant"))),

            // Field tag used in place of type name (in same sum)
            TestCase("(define some_domain (domain (sum some_sum (a_variant (a_field int)) (b_variant x::a_field))))",
                     makeErr(1, 80, SemanticErrorContext.UndefinedType("a_field"))),

            // Variant identifier used in place of type name (in a different sum)
            TestCase("(define some_domain (domain (sum some_sum av::(a_variant)) (sum another_sum (b_variant x::av))))",
                     makeErr(1, 88, SemanticErrorContext.UndefinedType("av"))),

            // Field identifier used in place of type name (in same sum)
            TestCase("(define some_domain (domain (sum some_sum (a_variant af::(a_field int)) (b_variant x::af))))",
                     makeErr(1, 84, SemanticErrorContext.UndefinedType("af"))),

            // Duplicate domain name
            TestCase("(define foo (domain)) (define foo (permute_domain foo))",
                     makeErr(1, 35, SemanticErrorContext.DuplicateTypeDomainName("foo"))),

            // Duplicate product tag
            TestCase("(define some_domain (domain (product some_product x::int) (product some_product)))",
                     makeErr(1, 59, SemanticErrorContext.NameAlreadyUsed("some_product", "some_domain"))),

            // Duplicate sum tag
            TestCase("(define some_domain (domain (sum dup_sum (variant)) (sum dup_sum (variant))))",
                     makeErr(1, 53, SemanticErrorContext.NameAlreadyUsed("dup_sum", "some_domain"))),

            // Duplicate sum has same name as product
            TestCase("(define some_domain (domain (sum dup_tag (variant)) (product dup_tag x::int)))",
                     makeErr(1, 53, SemanticErrorContext.NameAlreadyUsed("dup_tag", "some_domain")))
        )
        @JvmStatic
        @Suppress("unused")
        fun parametersForNameErrorsTest2() = listOf(
            // Duplicate product has same tag as sum
            TestCase("(define some_domain (domain (product dup_tag) (sum dup_tag)))",
                     makeErr(1, 47, SemanticErrorContext.NameAlreadyUsed("dup_tag", "some_domain"))),

            // Duplicate sum variant (same sum)
            TestCase("(define some_domain (domain (sum some_sum (a x::int) (a y::int))))",
                     makeErr(1, 54, SemanticErrorContext.NameAlreadyUsed("a", "some_domain"))),

            // Duplicate sum variant (different sums)
            TestCase("(define some_domain (domain (sum some_sum (dup_variant x::int)) (sum another_sum (dup_variant y::int))))",
                     makeErr(1, 82, SemanticErrorContext.NameAlreadyUsed("dup_variant", "some_domain"))),

            // Sum variant uses same tag as sum
            TestCase("(define some_domain (domain (sum dup_tag (dup_tag))))",
                     makeErr(1, 42, SemanticErrorContext.NameAlreadyUsed("dup_tag", "some_domain"))),

            // Sum variant uses same tag as other type in same domain
            TestCase("(define some_domain (domain (product some_product) (sum some_sum (some_product))))",
                     makeErr(1, 66, SemanticErrorContext.NameAlreadyUsed("some_product", "some_domain"))),

            // Duplicate record element tag within same sum variant record
            TestCase("(define some_domain (domain (sum some_sum (a_variant (some_field int) (some_field int)))))",
                     makeErr(1, 71, SemanticErrorContext.DuplicateRecordElementTag("some_field"))),

            // Duplicate record element tag within same record type
            TestCase("(define some_domain (domain (record some_record (some_field int) (some_field int))))",
                     makeErr(1, 66, SemanticErrorContext.DuplicateRecordElementTag("some_field"))),

            // Duplicate product element tag
            TestCase("(define some_domain (domain (product some_product some_field::int some_field::int)))",
                     makeErr(1, 67, SemanticErrorContext.DuplicateElementIdentifier("some_field"))),

            // Duplicate product element identifier
            TestCase("(define some_domain (domain (product some_product some_field::int some_field::int)))",
                     makeErr(1, 67, SemanticErrorContext.DuplicateElementIdentifier("some_field"))),

            // Duplicate record element identifier (duplicates other identifier)
            TestCase("(define some_domain (domain (record some_record x::(x int) x::(y int))))",
                     makeErr(1, 60, SemanticErrorContext.DuplicateElementIdentifier("x"))),

            // Duplicate record element identifier (duplicates other tag)
            TestCase("(define some_domain (domain (record some_record (a int) a::(c int))))",
                     makeErr(1, 57, SemanticErrorContext.DuplicateElementIdentifier("a"))),

            // Record with no fields
            TestCase("(define some_domain (domain (record some_record)))",
                     makeErr(1, 29, SemanticErrorContext.EmptyRecord)))
    }
}

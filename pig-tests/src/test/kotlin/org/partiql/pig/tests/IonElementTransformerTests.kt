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

package org.partiql.pig.tests

import com.amazon.ionelement.api.loadSingleElement
import com.amazon.ionelement.api.metaContainerOf
import com.amazon.ionelement.api.withMetas
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.runtime.DomainNode
import org.partiql.pig.runtime.asPrimitive
import org.partiql.pig.tests.generated.TestDomain

/**
 * Tests all of the equivalence classes outlined in the comment above `TestDomain` defined in
 * `type-domains/sample-universe.ion`
 */
class IonElementTransformerTests {

    @Test
    fun `int primitives preserve metas round trip`() {
        val input = TestDomain.build {
            intPair_(
                1.asPrimitive().withMeta("life", 42),
                2.asPrimitive().withMeta("so long", "thanks for all the fish")
            )
        }

        val element = input.toIonElement()

        // Note: we are asserting only that metas are preserved here.
        // (There are other tests to ensure that these domain types are serialized correctly.)

        // Assert that metas were added to the IonElements during serialization
        assertEquals(42, element.values[1].metas["life"])
        assertEquals("thanks for all the fish", element.values[2].metas["so long"])

        val output = TestDomain.transform(element) as TestDomain.IntPair

        // Assert that metas were added to the domain elements during transformation
        assertEquals(42, output.first.metas["life"])
        assertEquals("thanks for all the fish", output.second.metas["so long"])
    }

    @Test
    fun `symbol primitives preserve metas round trip`() {
        val input = TestDomain.build {
            symbolPair_(
                "foo".asPrimitive(metaContainerOf("life" to 42)),
                "bar".asPrimitive(metaContainerOf("so long" to "thanks for all the fish"))
            )
        }

        val element = input.toIonElement()

        // Note: we are asserting only that metas are preserved here.
        // (There are other tests to ensure that these domain types are serialized correctly.)

        // Assert that metas were added to the IonElements during serialization
        assertEquals(42, element.values[1].metas["life"])
        assertEquals("thanks for all the fish", element.values[2].metas["so long"])

        val output = TestDomain.transform(element) as TestDomain.SymbolPair

        // Assert that metas were added to the domain elements during deserialization
        assertEquals(42, output.first.metas["life"])
        assertEquals("thanks for all the fish", output.second.metas["so long"])
    }

    data class TestCase(val expectedDomainInstance: DomainNode, val expectedIonText: String)

    @ParameterizedTest
    @ArgumentsSource(AllElementTypesTestArgumentProvider::class)
    fun allElementTypesTest(tc: TestCase) = runTestCase(tc)
    class AllElementTypesTestArgumentProvider : ArgumentsProviderBase() {
        @Suppress("BooleanLiteralArgument")
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                TestDomain.build { boolPair(true, true) },
                "(bool_pair true true)"
            ),
            TestCase(
                TestDomain.build { boolPair(true, false) },
                "(bool_pair true false)"
            ),
            TestCase(
                TestDomain.build { boolPair(false, true) },
                "(bool_pair false true)"
            ),
            TestCase(
                TestDomain.build { boolPair(false, false) },
                "(bool_pair false false)"
            ),
            TestCase(
                TestDomain.build { intPair(1, 2) },
                "(int_pair 1 2)"
            ),
            TestCase(
                TestDomain.build { symbolPair("foo", "bar") },
                "(symbol_pair foo bar)"
            ),
            TestCase(
                TestDomain.build { intSymbolPair(123, "bar") },
                "(int_symbol_pair 123 bar)"
            ),
            TestCase(
                TestDomain.build { symbolIntPair("bar", 123) },
                "(symbol_int_pair bar 123)"
            ),
            TestCase(
                TestDomain.build { intPairPair(intPair(1, 2), intPair(3, 4)) },
                "(int_pair_pair (int_pair 1 2) (int_pair 3 4))"
            ),
            TestCase(
                TestDomain.build { symbolPairPair(symbolPair("foo", "bar"), symbolPair("bat", "baz")) },
                "(symbol_pair_pair (symbol_pair foo bar) (symbol_pair bat baz))"
            ),
            TestCase(
                TestDomain.build { answerPair(yes(), no()) },
                "(answer_pair (yes) (no))"
            ),
            TestCase(
                TestDomain.build { answerPair(no(), yes()) },
                "(answer_pair (no) (yes))"
            ),
            TestCase(
                TestDomain.build { answerIntPair(yes(), 123) },
                "(answer_int_pair (yes) 123)"
            ),
            TestCase(
                TestDomain.build { answerIntPair(no(), 123) },
                "(answer_int_pair (no) 123)"
            ),
            TestCase(
                TestDomain.build { intAnswerPair(456, yes()) },
                "(int_answer_pair 456 (yes))"
            ),
            TestCase(
                TestDomain.build { intAnswerPair(456, no()) },
                "(int_answer_pair 456 (no))"
            ),
            TestCase(
                TestDomain.build { answerSymbolPair(yes(), "bob") },
                "(answer_symbol_pair (yes) bob)"
            ),
            TestCase(
                TestDomain.build { answerSymbolPair(no(), "bob") },
                "(answer_symbol_pair (no) bob)"
            ),
            TestCase(
                TestDomain.build { symbolAnswerPair("betty", yes()) },
                "(symbol_answer_pair betty (yes))"
            ),
            TestCase(
                TestDomain.build { symbolAnswerPair("betty", no()) },
                "(symbol_answer_pair betty (no))"
            ),
            TestCase(
                TestDomain.build { recursivePair(123) },
                "(recursive_pair 123 null)"
            ),
            TestCase(
                TestDomain.build { recursivePair(123, recursivePair(456)) },
                "(recursive_pair 123 (recursive_pair 456 null))"
            ),
            TestCase(
                TestDomain.build { domainLevelRecord(42, "fourty-three") },
                "(domain_level_record (some_field 42) (another_field 'fourty-three'))"
            ),
            TestCase(
                TestDomain.build { domainLevelRecord(42, "fourty-three", 44) },
                "(domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44))"
            ),
            TestCase(
                TestDomain.build { productWithRecord(41, domainLevelRecord(42, "fourty-three", 44)) },
                "(product_with_record 41 (domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44)))"
            ),
            TestCase(
                TestDomain.build { variantWithRecord(41, domainLevelRecord(42, "fourty-three", 44)) },
                "(variant_with_record 41 (domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44)))"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(VariadicElementssTestArgumentsProvider::class)
    fun variadicElements(tc: TestCase) = runTestCase(tc)
    class VariadicElementssTestArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // variadicMin0 has a single variadic element with no minimum arity

            TestCase(
                TestDomain.build { variadicMin0(listOf()) },
                "(variadic_min_0)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(listOf(1L)) },
                "(variadic_min_0 1)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(listOf(1L, 2L)) },
                "(variadic_min_0 1 2)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(listOf(1L, 2L, 3L)) },
                "(variadic_min_0 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                TestDomain.build { variadicMin0() },
                "(variadic_min_0)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(1L) },
                "(variadic_min_0 1)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(1L, 2L) },
                "(variadic_min_0 1 2)"
            ),
            TestCase(
                TestDomain.build { variadicMin0(1L, 2L, 3L) },
                "(variadic_min_0 1 2 3)"
            ),

            // variadicMin1 has a single variadic element with a minimum arity of 1

            TestCase(
                TestDomain.build { variadicMin1(listOf(1L)) },
                "(variadic_min_1 1)"
            ),
            TestCase(
                TestDomain.build { variadicMin1(listOf(1L, 2L)) },
                "(variadic_min_1 1 2)"
            ),
            TestCase(
                TestDomain.build { variadicMin1(listOf(1L, 2L, 3L)) },
                "(variadic_min_1 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                TestDomain.build { variadicMin1(1L) },
                "(variadic_min_1 1)"
            ),
            TestCase(
                TestDomain.build { variadicMin1(1L, 2L) },
                "(variadic_min_1 1 2)"
            ),
            TestCase(
                TestDomain.build { variadicMin1(1L, 2L, 3L) },
                "(variadic_min_1 1 2 3)"
            ),

            // elementVariadic

            TestCase(
                TestDomain.build { elementVariadic("boo", listOf(1L)) },
                "(element_variadic boo 1)"
            ),
            TestCase(
                TestDomain.build { elementVariadic("loo", listOf(1L, 2L)) },
                "(element_variadic loo 1 2)"
            ),
            TestCase(
                TestDomain.build { elementVariadic("noo", listOf(1L, 2L, 3L)) },
                "(element_variadic noo 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                TestDomain.build { elementVariadic("coo", 1L) },
                "(element_variadic coo 1)"
            ),
            TestCase(
                TestDomain.build { elementVariadic("snoo", 1L, 2L) },
                "(element_variadic snoo 1 2)"
            ),
            TestCase(
                TestDomain.build { elementVariadic("moo", 1L, 2L, 3L) },
                "(element_variadic moo 1 2 3)"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(OptionalElementsTestArgumentsProvider::class)
    fun optionalDataTypes(tc: TestCase) = runTestCase(tc)
    class OptionalElementsTestArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // optional1

            TestCase(
                TestDomain.build { optional1(1L) },
                "(optional_1 1)"
            ),
            TestCase(
                TestDomain.build { optional1(null) },
                "(optional_1 null)"
            ),
            // Using default value for argument.
            TestCase(
                TestDomain.build { optional1() },
                "(optional_1 null)"
            ),

            // optional2

            TestCase(
                TestDomain.build { optional2(null, null) },
                "(optional_2 null null)"
            ),
            TestCase(
                TestDomain.build { optional2(null, 2) },
                "(optional_2 null 2)"
            ),
            TestCase(
                TestDomain.build { optional2(1, null) },
                "(optional_2 1 null)"
            ),
            TestCase(
                TestDomain.build { optional2(1L, 2L) },
                "(optional_2 1 2)"
            ),
            // Using default value for argument.
            TestCase(
                TestDomain.build { optional2() },
                "(optional_2 null null)"
            ),
            TestCase(
                TestDomain.build { optional2(1L) },
                "(optional_2 1 null)"
            ),
            TestCase(
                TestDomain.build { optional2(second = 2L) },
                "(optional_2 null 2)"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(RequiredOptionalVariadicTestArgumentsProvider::class)
    fun requiredOptionalVariadic(tc: TestCase) = runTestCase(tc)
    class RequiredOptionalVariadicTestArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // required_optional

            TestCase(
                TestDomain.build { requiredOptional(1L, 1L) },
                "(required_optional 1 1)"
            ),
            TestCase(
                TestDomain.build { requiredOptional(1L, null) },
                "(required_optional 1 null)"
            ),
            TestCase(
                TestDomain.build { requiredOptional(1L) },
                "(required_optional 1 null)"
            ),

            // optional_required

            TestCase(
                TestDomain.build { optionalRequired(1L, 1L) },
                "(optional_required 1 1)"
            ),
            TestCase(
                TestDomain.build { optionalRequired(null, 1L) },
                "(optional_required null 1)"
            ),
            TestCase(
                TestDomain.build { optionalRequired(second = 1L) },
                "(optional_required null 1)"
            ),

            // required_variadic

            TestCase(
                TestDomain.build { requiredVariadic(0L, listOf()) },
                "(required_variadic 0)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, listOf(1L)) },
                "(required_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, listOf(1L, 2L)) },
                "(required_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, listOf(1L, 2L, 3L)) },
                "(required_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L) },
                "(required_variadic 0)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, 1L) },
                "(required_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, 1L, 2L) },
                "(required_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { requiredVariadic(0L, 1L, 2L, 3L) },
                "(required_variadic 0 1 2 3)"
            ),

            // optional_variadic

            TestCase(
                TestDomain.build { optionalVariadic(0L, listOf()) },
                "(optional_variadic 0)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, listOf(1L)) },
                "(optional_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, listOf(1L, 2L)) },
                "(optional_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, listOf(1L, 2L, 3L)) },
                "(optional_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L) },
                "(optional_variadic 0)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, 1L) },
                "(optional_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, 1L, 2L) },
                "(optional_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(0L, 1L, 2L, 3L) },
                "(optional_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(null, listOf(1L, 2L, 3L)) },
                "(optional_variadic null 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(null, 1L, 2L, 3L) },
                "(optional_variadic null 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(null) },
                "(optional_variadic null)"
            ),
            TestCase(
                TestDomain.build { optionalVariadic(second = listOf(1L)) },
                "(optional_variadic null 1)"
            ),

            // required_optional_variadic

            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, listOf()) },
                "(required_optional_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, listOf(2L)) },
                "(required_optional_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, listOf(2L, 3L)) },
                "(required_optional_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, listOf(2L, 3L, 4L)) },
                "(required_optional_variadic 0 1 2 3 4)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L) },
                "(required_optional_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, 2L) },
                "(required_optional_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, 2L, 3L) },
                "(required_optional_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, 1L, 2L, 3L, 4L) },
                "(required_optional_variadic 0 1 2 3 4)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, null, listOf(1L, 2L, 3L)) },
                "(required_optional_variadic 0 null 1 2 3)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, null, 1L, 2L, 3L) },
                "(required_optional_variadic 0 null 1 2 3)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, null) },
                "(required_optional_variadic 0 null)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L) },
                "(required_optional_variadic 0 null)"
            ),
            TestCase(
                TestDomain.build { requiredOptionalVariadic(0L, third = listOf(1L)) },
                "(required_optional_variadic 0 null 1)"
            ),

            // optional_required_variadic

            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, listOf()) },
                "(optional_required_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, listOf(2L)) },
                "(optional_required_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, listOf(2L, 3L)) },
                "(optional_required_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, listOf(2L, 3L, 4L)) },
                "(optional_required_variadic 0 1 2 3 4)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L) },
                "(optional_required_variadic 0 1)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, 2L) },
                "(optional_required_variadic 0 1 2)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, 2L, 3L) },
                "(optional_required_variadic 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(0L, 1L, 2L, 3L, 4L) },
                "(optional_required_variadic 0 1 2 3 4)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(null, 0L, listOf(1L, 2L, 3L)) },
                "(optional_required_variadic null 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(null, 0L, 1L, 2L, 3L) },
                "(optional_required_variadic null 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(null, 0L) },
                "(optional_required_variadic null 0)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(second = 0L, third = listOf(1L, 2L, 3L)) },
                "(optional_required_variadic null 0 1 2 3)"
            ),
            TestCase(
                TestDomain.build { optionalRequiredVariadic(second = 0L) },
                "(optional_required_variadic null 0)"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(SumTestArgumentsProvider::class)
    fun sumTests(tc: TestCase) {
        runTestCase(tc)
    }

    class SumTestArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                TestDomain.build { entityPair(slug(), slug()) },
                "(entity_pair (slug) (slug))"
            ),
            TestCase(
                TestDomain.build { entityPair(android(123), slug()) },
                "(entity_pair (android 123) (slug))"
            ),
            TestCase(
                TestDomain.build { entityPair(slug(), android(456)) },
                "(entity_pair (slug) (android 456))"
            ),
            TestCase(
                TestDomain.build {
                    entityPair(
                        android(789),
                        human(firstName = "billy", middleNames = listOf("joe", "johnson", "jack"), lastName = "bob")
                    )
                },
                """
                    (entity_pair 
                        (android 789) 
                        (human 
                            (first_name billy)
                            (middle_names joe johnson jack)
                            (last_name bob)))
                """
            ),
            TestCase(
                TestDomain.build {
                    human(
                        title = "mister",
                        firstName = "joe",
                        lastName = "schmoe",
                        parent = slug()
                    )
                },
                """
                    (human 
                        (first_name joe) 
                        (last_name schmoe)
                        (title mister)
                        (parent (slug)))
                """
            ),
            TestCase(
                TestDomain.build { entityPair(slug(), slug()) },
                "(entity_pair (slug) (slug))"
            )
        )
    }

    private fun runTestCase(tc: TestCase) {
        // Transform tc.expectedIonText first.  If this fails it's a problem with the test
        // case, not the (de)serialization code.
        val expectedIonElement = assertDoesNotThrow("Check #1: expectedIonText must parse") {
            loadSingleElement(tc.expectedIonText)
        }

        // Deserialize and assert that the result is as expected.
        val actualTransformed = TestDomain.transform(expectedIonElement)

        assertEquals(
            tc.expectedDomainInstance,
            actualTransformed,
            "Check #2: The expected domain type instance and the transformed instance must match."
        )

        assertEquals(
            tc.expectedDomainInstance.hashCode(),
            actualTransformed.hashCode(),
            "Check #3: The hash codes of the expected domain type and transformed instances must match. "
        )

        // Serialize and assert that the result is as expected.
        val actualSerialized = tc.expectedDomainInstance.toIonElement()
        assertEquals(
            expectedIonElement,
            actualSerialized,
            "Check #4: The expected serialized domain type and the serialized instance must match"
        )

        // Add some metas to expectedIonElement, transform again and verify that the metas are present
        val expectedIonElementWithMetas = expectedIonElement.withMetas(metaContainerOf("foo" to 1, "bar" to 2))
        val transformedWithMetas = TestDomain.transform(expectedIonElementWithMetas)
        assertEquals(2, transformedWithMetas.metas.size)
        assertEquals(1, transformedWithMetas.metas["foo"])
        assertEquals(2, transformedWithMetas.metas["bar"])

        // Do the inverse
        val actualIonElementWithMetas = transformedWithMetas.toIonElement()
        assertEquals(2, actualIonElementWithMetas.metas.size)
        assertEquals(1, actualIonElementWithMetas.metas["foo"])
        assertEquals(2, actualIonElementWithMetas.metas["bar"])
    }
}

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

import com.amazon.ionelement.api.createIonElementLoader
import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.runtime.DomainNode
import org.partiql.pig.runtime.asPrimitive
import org.partiql.pig.tests.generated.test_domain

/**
 * Tests all of the equivalence classes outlined in the comment above `test_domain` defined in
 * `type-domains/sample-universe.ion`
 */
class IonElementTransformerTests {

    @Test
    fun `int primitives preserve metas round trip`() {
        val input = test_domain.build {
            int_pair_(
                1.asPrimitive().withMeta("life", 42),
                2.asPrimitive().withMeta("so long", "thanks for all the fish"))
        }

        val element = input.toIonElement().sexpValue

        // Note: we are asserting only that metas are preserved here.
        // (There are other tests to ensure that these domain types are serialized correctly.)

        // Assert that metas were added to the IonElements during serialization
        assertEquals(42, element[1].metas["life"])
        assertEquals("thanks for all the fish", element[2].metas["so long"])

        val output = test_domain.transform(element) as test_domain.int_pair

        // Assert that metas were added to the domain elements during transformation
        assertEquals(42, output.int0.metas["life"])
        assertEquals("thanks for all the fish", output.int1.metas["so long"])
    }

    @Test
    fun `symbol primitives preserve metas round trip`() {
        val input = test_domain.build {
            symbol_pair_(
                "foo".asPrimitive(metaContainerOf("life" to 42)),
                "bar".asPrimitive(metaContainerOf("so long" to "thanks for all the fish")))
        }

        val element = input.toIonElement().sexpValue

        // Note: we are asserting only that metas are preserved here.
        // (There are other tests to ensure that these domain types are serialized correctly.)

        // Assert that metas were added to the IonElements during serialization
        assertEquals(42, element[1].metas["life"])
        assertEquals("thanks for all the fish", element[2].metas["so long"])

        val output = test_domain.transform(element) as test_domain.symbol_pair

        // Assert that metas were added to the domain elements during deserialization
        assertEquals(42, output.symbol0.metas["life"])
        assertEquals("thanks for all the fish", output.symbol1.metas["so long"])
    }

    data class TestCase(val expectedDomainInstance: DomainNode, val expectedIonText: String)

    @ParameterizedTest
    @ArgumentsSource(AllElementTypesTestArgumentProvider::class)
    fun allElementTypesTest(tc: TestCase) = runTestCase(tc)
    class AllElementTypesTestArgumentProvider: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                test_domain.build { int_pair(1, 2) },
                "(int_pair 1 2)"
            ),
            TestCase(
                test_domain.build { symbol_pair("foo", "bar") },
                "(symbol_pair foo bar)"
            ),
            TestCase(
                test_domain.build { int_symbol_pair(123, "bar") },
                "(int_symbol_pair 123 bar)"
            ),
            TestCase(
                test_domain.build { symbol_int_pair("bar", 123) },
                "(symbol_int_pair bar 123)"
            ),
            TestCase(
                test_domain.build { int_pair_pair(int_pair(1, 2), int_pair(3, 4)) },
                "(int_pair_pair (int_pair 1 2) (int_pair 3 4))"
            ),
            TestCase(
                test_domain.build { symbol_pair_pair(symbol_pair("foo", "bar"), symbol_pair("bat", "baz")) },
                "(symbol_pair_pair (symbol_pair foo bar) (symbol_pair bat baz))"
            ),
            TestCase(
                test_domain.build { answer_pair(yes(), no()) },
                "(answer_pair (yes) (no))"
            ),
            TestCase(
                test_domain.build { answer_pair(no(), yes()) },
                "(answer_pair (no) (yes))"
            ),
            TestCase(
                test_domain.build { answer_int_pair(yes(), 123) },
                "(answer_int_pair (yes) 123)"
            ),
            TestCase(
                test_domain.build { answer_int_pair(no(), 123) },
                "(answer_int_pair (no) 123)"
            ),
            TestCase(
                test_domain.build { int_answer_pair(456, yes()) },
                "(int_answer_pair 456 (yes))"
            ),
            TestCase(
                test_domain.build { int_answer_pair(456, no()) },
                "(int_answer_pair 456 (no))"
            ),
            TestCase(
                test_domain.build { answer_symbol_pair(yes(), "bob") },
                "(answer_symbol_pair (yes) bob)"
            ),
            TestCase(
                test_domain.build { answer_symbol_pair(no(), "bob") },
                "(answer_symbol_pair (no) bob)"
            ),
            TestCase(
                test_domain.build { symbol_answer_pair("betty", yes()) },
                "(symbol_answer_pair betty (yes))"
            ),
            TestCase(
                test_domain.build { symbol_answer_pair("betty", no()) },
                "(symbol_answer_pair betty (no))"
            ),
            TestCase(
                test_domain.build { recursive_pair(123) },
                "(recursive_pair 123 null)"
            ),
            TestCase(
                test_domain.build { recursive_pair(123, recursive_pair(456)) },
                "(recursive_pair 123 (recursive_pair 456 null))"
            ),
            TestCase(
                test_domain.build { domain_level_record(42, "fourty-three") },
                "(domain_level_record (some_field 42) (another_field 'fourty-three'))"
            ),
            TestCase(
                test_domain.build { domain_level_record(42, "fourty-three", 44) },
                "(domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44))"
            ),
            TestCase(
                test_domain.build { product_with_record(41, domain_level_record(42, "fourty-three", 44)) },
                "(product_with_record 41 (domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44)))"
            ),
            TestCase(
                test_domain.build { variant_with_record(41, domain_level_record(42, "fourty-three", 44)) },
                "(variant_with_record 41 (domain_level_record (some_field 42) (another_field 'fourty-three') (optional_field 44)))"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(VariadicElementssTestArgumentsProvider::class)
    fun variadicElements(tc: TestCase) = runTestCase(tc)
    class VariadicElementssTestArgumentsProvider: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // variadic_min_0 has a single variadic element with no minimum arity

            TestCase(
                test_domain.build { variadic_min_0(listOf()) },
                "(variadic_min_0)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(listOf(1L)) },
                "(variadic_min_0 1)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(listOf(1L, 2L)) },
                "(variadic_min_0 1 2)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(listOf(1L, 2L, 3L)) },
                "(variadic_min_0 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                test_domain.build { variadic_min_0() },
                "(variadic_min_0)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(1L) },
                "(variadic_min_0 1)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(1L, 2L) },
                "(variadic_min_0 1 2)"
            ),
            TestCase(
                test_domain.build { variadic_min_0(1L, 2L, 3L) },
                "(variadic_min_0 1 2 3)"
            ),

            // variadic_min_1 has a single variadic element with a minimum arity of 1

            TestCase(
                test_domain.build { variadic_min_1(listOf(1L)) },
                "(variadic_min_1 1)"
            ),
            TestCase(
                test_domain.build { variadic_min_1(listOf(1L, 2L)) },
                "(variadic_min_1 1 2)"
            ),
            TestCase(
                test_domain.build { variadic_min_1(listOf(1L, 2L, 3L)) },
                "(variadic_min_1 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                test_domain.build { variadic_min_1(1L) },
                "(variadic_min_1 1)"
            ),
            TestCase(
                test_domain.build { variadic_min_1(1L, 2L) },
                "(variadic_min_1 1 2)"
            ),
            TestCase(
                test_domain.build { variadic_min_1(1L, 2L, 3L) },
                "(variadic_min_1 1 2 3)"
            ),

            // element_variadic

            TestCase(
                test_domain.build { element_variadic("boo", listOf(1L)) },
                "(element_variadic boo 1)"
            ),
            TestCase(
                test_domain.build { element_variadic("loo", listOf(1L, 2L)) },
                "(element_variadic loo 1 2)"
            ),
            TestCase(
                test_domain.build { element_variadic("noo", listOf(1L, 2L, 3L)) },
                "(element_variadic noo 1 2 3)"
            ),
            // same as above but using alternate constructor
            TestCase(
                test_domain.build { element_variadic("coo", 1L) },
                "(element_variadic coo 1)"
            ),
            TestCase(
                test_domain.build { element_variadic("snoo", 1L, 2L) },
                "(element_variadic snoo 1 2)"
            ),
            TestCase(
                test_domain.build { element_variadic("moo", 1L, 2L, 3L) },
                "(element_variadic moo 1 2 3)"
            )
        )
    }



    @ParameterizedTest
    @ArgumentsSource(OptionalElementsTestArgumentsProvider::class)
    fun optionalDataTypes(tc: TestCase) = runTestCase(tc)
    class OptionalElementsTestArgumentsProvider: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // optional_1

            TestCase(
                test_domain.build { optional_1(1L) },
                "(optional_1 1)"
            ),
            TestCase(
                test_domain.build { optional_1(null) },
                "(optional_1 null)"
            ),
            // Using default value for argument.
            TestCase(
                test_domain.build { optional_1() },
                "(optional_1 null)"
            ),

            // optional_2

            TestCase(
                test_domain.build { optional_2(null, null) },
                "(optional_2 null null)"
            ),
            TestCase(
                test_domain.build { optional_2(null, 2) },
                "(optional_2 null 2)"
            ),
            TestCase(
                test_domain.build { optional_2(1, null) },
                "(optional_2 1 null)"
            ),
            TestCase(
                test_domain.build { optional_2(1L, 2L) },
                "(optional_2 1 2)"
            ),
            // Using default value for argument.
            TestCase(
                test_domain.build { optional_2() },
                "(optional_2 null null)"
            ),
            TestCase(
                test_domain.build { optional_2(1L) },
                "(optional_2 1 null)"
            ),
            TestCase(
                test_domain.build { optional_2(int1 = 2L) },
                "(optional_2 null 2)"
            ))
    }

    @ParameterizedTest
    @ArgumentsSource(SumTestArgumentsProvider::class)
    fun sumTests(tc: TestCase) {
        runTestCase(tc)
    }

    class SumTestArgumentsProvider: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                test_domain.build { entity_pair(slug(), slug()) },
                "(entity_pair (slug) (slug))"
            ),
            TestCase(
                test_domain.build { entity_pair(android(123), slug()) },
                "(entity_pair (android 123) (slug))"
            ),
            TestCase(
                test_domain.build { entity_pair(slug(), android(456)) },
                "(entity_pair (slug) (android 456))"
            ),
            TestCase(
                test_domain.build {
                    entity_pair(
                        android(789),
                        human(first_name = "billy", last_name = "bob"))
                },
                """
                    (entity_pair 
                        (android 789) 
                        (human 
                            (first_name billy) 
                            (last_name bob)))
                """
            ),
            TestCase(
                test_domain.build {
                    human(
                        title = "mister",
                        first_name = "joe",
                        last_name = "schmoe",
                        parent = slug())
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
                test_domain.build { entity_pair(slug(), slug()) },
                "(entity_pair (slug) (slug))"
            )
        )
    }

    private fun runTestCase(tc: TestCase) {
        // Transform tc.expectedIonText first.  If this fails it's a problem with the test
        // case, not the (de)serialization code.
        val expectedIonElement = assertDoesNotThrow("Check #1: expectedIonText must parse") {
            createIonElementLoader(false).loadSingleElement(tc.expectedIonText)
        }

        // Deserialize and assert that the result is as expected.
        val actualTransformed = test_domain.transform(expectedIonElement)

        assertEquals(
            tc.expectedDomainInstance,
            actualTransformed,
            "Check #2: The expected domain type instance and the transformed instance must match.")

        assertEquals(
            tc.expectedDomainInstance.hashCode(),
            actualTransformed.hashCode(),
            "Check #3: The hash codes of the expected domain type and transformed instances must match. ")

        // Serialize and assert that the result is as expected.
        val actualSerialized = tc.expectedDomainInstance.toIonElement()
        assertEquals(
            expectedIonElement,
            actualSerialized,
            "Check #4: The expected serialized domain type and the serialized instance must match")

        // Add some metas to expectedIonElement, transform again and verify that the metas are present
        val expectedIonElementWithMetas = expectedIonElement.withMetas(metaContainerOf("foo" to 1, "bar" to 2))
        val transformedWithMetas = test_domain.transform(expectedIonElementWithMetas)
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

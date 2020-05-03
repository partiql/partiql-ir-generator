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

import com.amazon.ionelement.api.ionString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.tests.generated.test_domain

/** Checks `equals` and `hashCode` implementations for generated types. */
class EqualityTests {

    @ParameterizedTest
    @ArgumentsSource(EqualityTestsArgumentsProvider::class)
    fun equalityCheckTest(tc: TestCase) {
        val left = tc.left
        val right = tc.right
        val isEqual = tc.isEqual
        assertEquality(isEqual, left, right)
        assertEquality(isEqual, left.withMeta("some_meta", 1), right.withMeta("some_meta", 1))
        assertEquality(isEqual, left.withMeta("some_meta", 1), right.withMeta("another_meta", 1))
    }

    private fun assertEquality(isEqual: Boolean, left: test_domain.test_domain_node, right: test_domain.test_domain_node) {
        if (isEqual) {
            assertEquals(left, right, "Domain instances should be equivalent")
            assertEquals(left.hashCode(), right.hashCode(), "Domain instances hash codes should be equal")
        } else {
            assertNotEquals(left, right, "Domain instances should *not* be equivalent")
            // It is unlikely but two different values can return the same hash code we might need to remove this
            // next assertion but it would be interesting to see if this assertion ever fails.
            assertNotEquals(left.hashCode(), right.hashCode(), "Domain instances hash codes should *not* be equal")
        }
    }

    data class TestCase(val left: test_domain.test_domain_node, val right: test_domain.test_domain_node, val isEqual: Boolean)
    class EqualityTestsArgumentsProvider: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            /////////////////////////////////////////////////
            // Products
            /////////////////////////////////////////////////
            TestCase(
                test_domain.build { int_pair(1, 1) },
                test_domain.build { int_pair(1, 1) },
                isEqual = true),
            TestCase(
                test_domain.build { int_pair(1, 1) },
                test_domain.build { int_pair(1, 2) },
                isEqual = false),
            TestCase(
                test_domain.build { int_pair(2, 1) },
                test_domain.build { int_pair(1, 2) },
                isEqual = false),

            TestCase(
                test_domain.build { symbol_pair("a", "a") },
                test_domain.build { symbol_pair("a", "a") },
                isEqual = true),
            TestCase(
                test_domain.build { symbol_pair("a", "a") },
                test_domain.build { symbol_pair("a", "b") },
                isEqual = false),
            TestCase(
                test_domain.build { symbol_pair("b", "a") },
                test_domain.build { symbol_pair("a", "b") },
                isEqual = false),

            TestCase(
                test_domain.build { ion_pair(ionString("a"), ionString("a")) },
                test_domain.build { ion_pair(ionString("a"), ionString("a")) },
                isEqual = true),
            TestCase(
                test_domain.build { ion_pair(ionString("a"), ionString("a")) },
                test_domain.build { ion_pair(ionString("a"), ionString("b")) },
                isEqual = false),
            TestCase(
                test_domain.build { ion_pair(ionString("b"), ionString("a")) },
                test_domain.build { ion_pair(ionString("a"), ionString("b")) },
                isEqual = false),


            /////////////////////////////////////////////////
            // Sum variants
            /////////////////////////////////////////////////
            TestCase(
                test_domain.build { yes() },
                test_domain.build { yes() },
                isEqual = true),
            TestCase(
                test_domain.build { no() },
                test_domain.build { no() },
                isEqual = true),

            TestCase(
                test_domain.build { yes() },
                test_domain.build { no() },
                isEqual = false),

            TestCase(
                test_domain.build { answer_pair(yes(), yes()) },
                test_domain.build { answer_pair(yes(), yes()) },
                isEqual = true),

            TestCase(
                test_domain.build { answer_pair(no(), no()) },
                test_domain.build { answer_pair(no(), no()) },
                isEqual = true),

            TestCase(
                test_domain.build { answer_pair(no(), yes()) },
                test_domain.build { answer_pair(no(), yes()) },
                isEqual = true),

            TestCase(
                test_domain.build { answer_pair(yes(), no()) },
                test_domain.build { answer_pair(yes(), no()) },
                isEqual = true),

            TestCase(
                test_domain.build { answer_pair(yes(), yes()) },
                test_domain.build { answer_pair(yes(), no()) },
                isEqual = false),

            TestCase(
                test_domain.build { answer_pair(yes(), no()) },
                test_domain.build { answer_pair(yes(), yes()) },
                isEqual = false),

            TestCase(
                test_domain.build { answer_pair(no(), yes()) },
                test_domain.build { answer_pair(yes(), yes()) },
                isEqual = false),

            TestCase(
                test_domain.build { answer_pair(yes(), yes()) },
                test_domain.build { answer_pair(no(), yes()) },
                isEqual = false),

            TestCase(
                test_domain.build { android(1) },
                test_domain.build { android(1) },
                isEqual = true),

            TestCase(
                test_domain.build { android(1) },
                test_domain.build { android(2) },
                isEqual = false),


            // Record with only required fields are specified
            TestCase(
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard") },
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard") },
                isEqual = true),
            TestCase(
                test_domain.build { human(first_name = "JeanLuc", last_name = "Picard") },
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard") },
                isEqual = false),

            // One record has optional field specified
            TestCase(
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard") },
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard", title = "Captain") },
                isEqual = false),

            // Optional field is different
            TestCase(
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard", title = "Captain") },
                test_domain.build { human(first_name = "Jean Luc", last_name = "Picard", title = "First Officer") },
                isEqual = false),

            // Recursive value is same
            TestCase(
                test_domain.build { human(first_name = "Bob", last_name = "Robert", parent = android(9)) },
                test_domain.build { human(first_name = "Bob", last_name = "Robert", parent = android(9)) },
                isEqual = true),

            // One record has a recursive field specified
            TestCase(
                test_domain.build { human(first_name = "Bob", last_name = "Robert") },
                test_domain.build { human(first_name = "Bob", last_name = "Robert", parent = android(9)) },
                isEqual = false),

            // Recursive value is different
            TestCase(
                test_domain.build { human(first_name = "Bob", last_name = "Robert", title = "Professional Nerd", parent = android(9)) },
                test_domain.build { human(first_name = "Bob", last_name = "Robert", title = "Professional Nerd", parent = android(10)) },
                isEqual = false)
        )
    }
}
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
import org.partiql.pig.tests.generated.TestDomain

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

    private fun assertEquality(isEqual: Boolean, left: TestDomain.TestDomainNode, right: TestDomain.TestDomainNode) {
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

    data class TestCase(val left: TestDomain.TestDomainNode, val right: TestDomain.TestDomainNode, val isEqual: Boolean)
    class EqualityTestsArgumentsProvider : ArgumentsProviderBase() {

        @Suppress("BooleanLiteralArgument")
        override fun getParameters(): List<Any> = listOf(
            // ///////////////////////////////////////////////
            // Products
            // ///////////////////////////////////////////////
            TestCase(
                TestDomain.build { boolPair(true, true) },
                TestDomain.build { boolPair(true, true) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { boolPair(true, true) },
                TestDomain.build { boolPair(true, false) },
                isEqual = false
            ),
            TestCase(
                TestDomain.build { boolPair(false, true) },
                TestDomain.build { boolPair(true, false) },
                isEqual = false
            ),
            TestCase(
                TestDomain.build { boolPair(true, false) },
                TestDomain.build { boolPair(false, true) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { intPair(1, 1) },
                TestDomain.build { intPair(1, 1) },
                isEqual = true
            ),
            TestCase(
                TestDomain.build { intPair(1, 1) },
                TestDomain.build { intPair(1, 2) },
                isEqual = false
            ),
            TestCase(
                TestDomain.build { intPair(2, 1) },
                TestDomain.build { intPair(1, 2) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { symbolPair("a", "a") },
                TestDomain.build { symbolPair("a", "a") },
                isEqual = true
            ),
            TestCase(
                TestDomain.build { symbolPair("a", "a") },
                TestDomain.build { symbolPair("a", "b") },
                isEqual = false
            ),
            TestCase(
                TestDomain.build { symbolPair("b", "a") },
                TestDomain.build { symbolPair("a", "b") },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { ionPair(ionString("a"), ionString("a")) },
                TestDomain.build { ionPair(ionString("a"), ionString("a")) },
                isEqual = true
            ),
            TestCase(
                TestDomain.build { ionPair(ionString("a"), ionString("a")) },
                TestDomain.build { ionPair(ionString("a"), ionString("b")) },
                isEqual = false
            ),
            TestCase(
                TestDomain.build { ionPair(ionString("b"), ionString("a")) },
                TestDomain.build { ionPair(ionString("a"), ionString("b")) },
                isEqual = false
            ),

            // ///////////////////////////////////////////////
            // Sum variants
            // ///////////////////////////////////////////////
            TestCase(
                TestDomain.build { yes() },
                TestDomain.build { yes() },
                isEqual = true
            ),
            TestCase(
                TestDomain.build { no() },
                TestDomain.build { no() },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { yes() },
                TestDomain.build { no() },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { answerPair(yes(), yes()) },
                TestDomain.build { answerPair(yes(), yes()) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { answerPair(no(), no()) },
                TestDomain.build { answerPair(no(), no()) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { answerPair(no(), yes()) },
                TestDomain.build { answerPair(no(), yes()) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { answerPair(yes(), no()) },
                TestDomain.build { answerPair(yes(), no()) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { answerPair(yes(), yes()) },
                TestDomain.build { answerPair(yes(), no()) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { answerPair(yes(), no()) },
                TestDomain.build { answerPair(yes(), yes()) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { answerPair(no(), yes()) },
                TestDomain.build { answerPair(yes(), yes()) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { answerPair(yes(), yes()) },
                TestDomain.build { answerPair(no(), yes()) },
                isEqual = false
            ),

            TestCase(
                TestDomain.build { android(1) },
                TestDomain.build { android(1) },
                isEqual = true
            ),

            TestCase(
                TestDomain.build { android(1) },
                TestDomain.build { android(2) },
                isEqual = false
            ),

            // Record with only required fields are specified
            TestCase(
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard") },
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard") },
                isEqual = true
            ),
            TestCase(
                TestDomain.build { human(firstName = "JeanLuc", lastName = "Picard") },
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard") },
                isEqual = false
            ),

            // One record has optional field specified
            TestCase(
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard") },
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard", title = "Captain") },
                isEqual = false
            ),

            // Optional field is different
            TestCase(
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard", title = "Captain") },
                TestDomain.build { human(firstName = "Jean Luc", lastName = "Picard", title = "First Officer") },
                isEqual = false
            ),

            // Recursive value is same
            TestCase(
                TestDomain.build { human(firstName = "Bob", lastName = "Robert", parent = android(9)) },
                TestDomain.build { human(firstName = "Bob", lastName = "Robert", parent = android(9)) },
                isEqual = true
            ),

            // One record has a recursive field specified
            TestCase(
                TestDomain.build { human(firstName = "Bob", lastName = "Robert") },
                TestDomain.build { human(firstName = "Bob", lastName = "Robert", parent = android(9)) },
                isEqual = false
            ),

            // Recursive value is different
            TestCase(
                TestDomain.build { human(firstName = "Bob", lastName = "Robert", title = "Professional Nerd", parent = android(9)) },
                TestDomain.build { human(firstName = "Bob", lastName = "Robert", title = "Professional Nerd", parent = android(10)) },
                isEqual = false
            )
        )
    }
}

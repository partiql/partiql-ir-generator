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

package org.partiql.pig.legacy.runtime

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.IonTextLocation
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class IntermediateRecordTests {
    private val oneOne = IonTextLocation(1, 1)

    @Test
    fun happyPath() {
        val someRecord = """
            (some_record
                (foo 1)
                (bar 2)
                (bat 3)
                (variadic_foo 4)
                (variadic_empty))
        """.trimIndent()

        val ir = createIntermediateRecord(someRecord)

        val foundFields = mutableListOf<IonElement>()

        // Required fields
        ir.processRequiredField("foo") { foundFields.add(it) }
        ir.processRequiredField("bar") { foundFields.add(it) }

        // Optional field that's present
        ir.processOptionalField("bat") { foundFields.add(it) }

        // Optional field that's not present
        ir.processOptionalField("baz") { foundFields.add(it) }

        // Variadic field with minimum arity of 1
        ir.processVariadicField("variadic_foo", 1) { foundFields.add(it) }

        // Variadic field with minimum arity of 0 that is present with no values
        // (should be removed so that the call to malformedIfAnyUnprocessedFieldsRemain does not throw)
        ir.processVariadicField("variadic_empty", 0) { foundFields.add(it) }

        // Variadic field with minimum arity of 0 that is not present (should not throw)
        ir.processVariadicField("variadic_not_present", 0) { fail("should not get called") }

        assertDoesNotThrow { ir.malformedIfAnyUnprocessedFieldsRemain() }
        assertEquals(listOf(ionInt(1), ionInt(2), ionInt(3), ionInt(4)), foundFields)
    }

    @ParameterizedTest
    @MethodSource("parametersForMalformedTest")
    fun malformedTests(tc: MalformedTestCase) {
        val ex = assertThrows<MalformedDomainDataException> {
            tc.blockThrowingMalformedDomainDataException()
        }
        tc.messageMustContainStrings.forEach {
            assertTrue(ex.message!!.contains(it), "exception message must contain '$it'")
        }
        assertEquals(oneOne, ex.location)
    }

    companion object {
        @JvmStatic
        @Suppress("UNUSED")
        fun parametersForMalformedTest() = listOf(
            MalformedTestCase(
                "required field missing",
                { createIntermediateRecord("(some_record (foo 1))").processRequiredField("bad_field") { fail("should not be called") } },
                listOf("bad_field")
            ),
            MalformedTestCase(
                "required field arity too high",
                { createIntermediateRecord("(some_record (foo 1 2))").processRequiredField("foo") { fail("should not be called") } },
                listOf("foo", "1..1")
            ),
            MalformedTestCase(
                "optional field arity too high",
                { createIntermediateRecord("(some_record (foo 1 2))").processOptionalField("foo") { fail("should not be called") } },
                listOf("foo", "1..1")
            ),
            MalformedTestCase(
                "variadic arity too low",
                { createIntermediateRecord("(some_record (foo 1 2))").processVariadicField("foo", 3) { fail("should not be called") } },
                listOf("foo", "3..2147483647")
            ),
            MalformedTestCase(
                "variadic arity too low",
                { createIntermediateRecord("(some_record (foo 1 2))").malformedIfAnyUnprocessedFieldsRemain() },
                listOf("Unexpected", "foo")
            )
        )

        private fun createIntermediateRecord(recordIonText: String): IntermediateRecord =
            loadSingleElement(recordIonText, IonElementLoaderOptions(includeLocationMeta = true))
                .asSexp()
                .transformToIntermediateRecord()

        data class MalformedTestCase(
            val name: String,
            val blockThrowingMalformedDomainDataException: () -> Unit,
            val messageMustContainStrings: List<String>
        )
    }
}

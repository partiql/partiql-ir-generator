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

package org.partiql.pig.runtime

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonTextLocation
import com.amazon.ionelement.api.createIonElementLoader
import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IntermediateRecordTests {
    private val oneOne = IonTextLocation(1, 1)

    @Test
    fun happyPath() {

        val someRecord = """
            (some_record
                (foo 1)
                (bar 2)
                (bat 3))
        """.trimIndent()

        val ir = createIonElementLoader(true).loadSingleElement(someRecord).asSexp().transformToIntermediateRecord()

        val foundFields = mutableListOf<IonElement>()

        // Required fields
        ir.processRequiredField("foo") { foundFields.add(it) }
        ir.processRequiredField("bar") { foundFields.add(it) }

        // Optional field that's present
        ir.processOptionalField("bat") { foundFields.add(it) }

        // Optional field that's not present
        ir.processOptionalField("baz") { foundFields.add(it) }

        assertDoesNotThrow { ir.malformedIfAnyUnprocessedFieldsRemain() }
        assertEquals(listOf(ionInt(1), ionInt(2), ionInt(3)), foundFields)
    }

    @Test
    fun requiredFieldMissing() {
        val ir = createIntermediateRecord(mapOf("foo" to ionInt(1).asAnyElement()))
        val ex = assertThrows<MalformedDomainDataException> {
            ir.processRequiredField("bad_field") { error("should not be invoked") }
        }
        assertTrue(ex.message!!.contains("bad_field"))
        assertEquals(oneOne, ex.location)
    }

    @Test
    fun extraFields() {
        val ir = createIntermediateRecord(mapOf("foo" to ionInt(1).asAnyElement()))
        val ex = assertThrows<MalformedDomainDataException>{ ir.malformedIfAnyUnprocessedFieldsRemain() }

        assertTrue(ex.message!!.contains("foo"))
        assertEquals(oneOne, ex.location)
    }


    private fun createIntermediateRecord(fields: Map<String, AnyElement>): IntermediateRecord =
        IntermediateRecord(
            recordTagName = "some_tag",
            location = oneOne,
            fields = fields)
}

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

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ErrorHelpersTests {
    // Note that "arity" is the number of elements following the tag.
    val someSexp = ionSexpOf(ionSymbol("some_tag"), ionInt(1), ionNull(ElementType.STRING)) // <-- has an arity of 2.

    @Test
    fun requireArityOrMalformed() {

        assertThrows<MalformedDomainDataException> {
            someSexp.requireArityOrMalformed(1)
        }.also {
            assertTrue(it.message!!.contains("1..1"))
        }

        assertDoesNotThrow {
            someSexp.requireArityOrMalformed(2)
        }

        assertThrows<MalformedDomainDataException> {
            someSexp.requireArityOrMalformed(3)
        }.also {
            assertTrue(it.message!!.contains("3..3"))
        }
    }

    @Test
    fun getRequired() {
        assertEquals(ionInt(1), someSexp.getRequired(0))

        assertThrows<MalformedDomainDataException> {
            someSexp.getRequired(1)
        }.also {
            assertTrue(it.message!!.contains("A non-null value is required."))
        }

        assertThrows<MalformedDomainDataException> {
            someSexp.getRequired(3)
        }.also {
            assertTrue(it.message!!.contains("index 3"))
        }
    }

    @Test
    fun getRequiredIon() {
        assertEquals(ionInt(1), someSexp.getRequiredIon(0))
        assertEquals(ionNull(ElementType.STRING), someSexp.getRequiredIon(1))

        assertThrows<MalformedDomainDataException> {
            someSexp.getRequiredIon(2)
        }.also {
            assertTrue(it.message!!.contains("index 2"))
        }
    }

    @Test
    fun getOptional() {
        assertEquals(ionInt(1), someSexp.getOptional(0))
        assertNull(someSexp.getOptional(1))
        assertThrows<MalformedDomainDataException> {
            someSexp.getOptional(2)
        }.also {
            assertTrue(it.message!!.contains("index 2"))
        }
    }
}

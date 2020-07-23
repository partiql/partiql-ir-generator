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

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LongPrimitiveTests {
    @Test
    fun copy() {
        val l = 42.asPrimitive(metaContainerOf("foo" to 43))
        assertEquals(42, l.value)
        assertEquals(metaContainerOf("foo" to 43), l.metas)

        val l2 = l.copy(value = 43)
        assertEquals(43, l2.value)
        assertEquals(metaContainerOf("foo" to 43), l2.metas)

        val l3 = l.copy(metas = metaContainerOf("foo" to 88))
        assertEquals(42, l3.value)
        assertEquals(metaContainerOf("foo" to 88), l3.metas)
    }

    @Test
    fun withMeta() {
        val l = 42.asPrimitive()
        assertEquals(42, l.value)
        assertEquals(emptyMetaContainer(), l.metas)

        val l2 = l.withMeta("foo", 42)
        assertEquals(42, l2.value)
        assertEquals(metaContainerOf("foo" to 42), l2.metas)
    }

    @Test
    fun toIonElement() {
        val elem = 42.asPrimitive(metaContainerOf("foo" to 43)).toIonElement()
        assertEquals(ionInt(42), elem)
        assertEquals(metaContainerOf("foo" to 43), elem.metas)
    }

    @Test
    fun toStringTest() {
        assertEquals("12345", 12345.asPrimitive().toString())
        assertEquals("12345", 12345.asPrimitive(metaContainerOf("foo" to 43)).toString())
    }

    @Test
    fun equalsAndHashCode() {
        val l1 = 42.asPrimitive()
        val l2 = 42.asPrimitive(metaContainerOf("foo" to 43))
        val l3 = 24.asPrimitive()

        // metas should not effect equivalence
        assertEquals(l1, l2)
        assertEquals(l1.hashCode(), l2.hashCode())

        assertNotEquals(l1, l3)
        assertNotEquals(l2, l3)
        // Note: hashCode is *likely* to be different, but it is not guaranteed, so therefore no assertion for that.
    }
}


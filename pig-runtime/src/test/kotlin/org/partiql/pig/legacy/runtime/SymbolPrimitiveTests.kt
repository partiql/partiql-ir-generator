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

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SymbolPrimitiveTests {
    @Test
    fun copy() {
        val s = "bat".asPrimitive(metaContainerOf("foo" to 43))
        assertEquals("bat", s.text)
        assertEquals(metaContainerOf("foo" to 43), s.metas)

        val s2 = s.copy(text = "baz")
        assertEquals("baz", s2.text)
        assertEquals(metaContainerOf("foo" to 43), s2.metas)

        val s3 = s.copy(metas = metaContainerOf("foo" to 88))
        assertEquals("bat", s3.text)
        assertEquals(metaContainerOf("foo" to 88), s3.metas)
    }

    @Test
    fun withMeta() {
        val s = "bat".asPrimitive()
        assertEquals("bat", s.text)
        assertEquals(emptyMetaContainer(), s.metas)

        val s2 = s.withMeta("foo", 42)
        assertEquals("bat", s2.text)
        assertEquals(metaContainerOf("foo" to 42), s2.metas)
    }

    @Test
    fun toIonElement() {
        val elem = "bat".asPrimitive(metaContainerOf("foo" to 43)).toIonElement()
        assertEquals(ionSymbol("bat"), elem)
        assertEquals(metaContainerOf("foo" to 43), elem.metas)
    }

    @Test
    fun toStringTest() {
        assertEquals("foobar", "foobar".asPrimitive().toString())
        assertEquals("foobar", "foobar".asPrimitive(metaContainerOf("foo" to 43)).toString())
    }

    @Test
    fun equalsAndHashCode() {
        val s1 = "bat".asPrimitive()
        val s2 = "bat".asPrimitive(metaContainerOf("foo" to 43))
        val s3 = "baz".asPrimitive()

        // metas should not effect equivalence
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())

        assertNotEquals(s1, s3)
        assertNotEquals(s2, s3)
        // Note: hashCode is *likely* to be different, but it is not guaranteed, so therefore no assertion for that.
    }
}

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

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.junit.jupiter.api.Test
import org.partiql.pig.runtime.LongPrimitive
import org.partiql.pig.runtime.asPrimitive
import org.partiql.pig.tests.generated.TestDomain
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

private const val NUMBER_KEY = "number"

class CopyTests {
    private val node = TestDomain.build { intPair(1, 2).withMeta(NUMBER_KEY, 3) }

    @Test
    fun `copy empty test`() {
        val copiedNode = node.copy()
        assertEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }

    @Test
    fun `copy with same values test`() {
        val copiedNode = node.copy(first = LongPrimitive(1, emptyMetaContainer()),
                                   second = LongPrimitive(2, emptyMetaContainer()),
                                   metas = metaContainerOf(NUMBER_KEY to 3))
        assertEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }

    @Test
    fun `copy element test`() {
        val copiedNode = node.copy(first = LongPrimitive(0, emptyMetaContainer()))

        assertNotEquals(node.first, copiedNode.first)
        assertEquals(node.second, copiedNode.second)
        assertEquals(node.metas, copiedNode.metas)
        assertNotEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }

    @Test
    fun `copy multiple elements test`() {
        val copiedNode = node.copy(first = LongPrimitive(0, emptyMetaContainer()),
                                   second = LongPrimitive(0, emptyMetaContainer()))

        assertNotEquals(node.first, copiedNode.first)
        assertNotEquals(node.second, copiedNode.second)
        assertEquals(node.metas, copiedNode.metas)
        assertNotEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }

    @Test
    fun `copy element twice test`() {
        val copiedNode = node.copy(first = LongPrimitive(0, emptyMetaContainer()))
        assertNotEquals(node, copiedNode)
        assertNotSame(node, copiedNode)

        val copiedAgainNode = node.copy(first = LongPrimitive(1, emptyMetaContainer()))
        assertEquals(node, copiedAgainNode)
        assertNotSame(node, copiedAgainNode)
        assertNotSame(copiedNode, copiedAgainNode)
    }

    @Test
    fun `copy metas - override with multiple parameters`() {
        val copiedNode = node.copy(second = 2L.asPrimitive(), metas = metaContainerOf(NUMBER_KEY to 0))
        assertNotEquals(node.metas, copiedNode.metas)
        assertEquals(node.first, copiedNode.first)
        assertEquals(2L, copiedNode.second.value)
        assertEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }

    @Test
    fun `copy test - override with single metas parameter`() {
        val copiedNode = node.copy(metaContainerOf(NUMBER_KEY to 0))
        assertNotEquals(node.metas, copiedNode.metas)
        assertEquals(node.first, copiedNode.first)
        assertEquals(node.second, copiedNode.second)
        assertEquals(node, copiedNode)
        assertNotSame(node, copiedNode)
    }
}

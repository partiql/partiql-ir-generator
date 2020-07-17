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

import com.amazon.ionelement.api.MetaContainer
import org.junit.jupiter.api.Test
import org.partiql.pig.runtime.LongPrimitive
import org.partiql.pig.tests.generated.TestDomain
import kotlin.test.assertEquals

private const val NUMBER_KEY = "number"
class FoldingVisitorTests {
    class DummyVisitor : TestDomain.FoldingVisitor<Long>() {
        override fun visitLongPrimitive(node: LongPrimitive, accumulator: Long): Long = accumulator + node.value
        override fun visitMetas(node: MetaContainer, accumulator: Long) =
            when {
                node.containsKey(NUMBER_KEY) -> accumulator + node[NUMBER_KEY] as Int
                else -> accumulator
            }
        }

    // Because the folding walker & visitor are stateless, the same instances may be reused for all tests
    private val foldingWalker = TestDomain.FoldingWalker(DummyVisitor())

    @Test
    fun visitProducts() {
        val node = TestDomain.build {
            intPairPair(intPair(1, 2), intPair(3, 4))
        }.withMeta(NUMBER_KEY, 5)

        val result = foldingWalker.walkIntPairPair(node, 0)
        assertEquals(15, result)
    }

    @Test
    fun visitRecords() {
        val node = TestDomain.build {
            domainLevelRecord(someField = 2, anotherField = "hi", optionalField = 3)
        }.withMeta("number", 4)

        val result = foldingWalker.walkDomainLevelRecord(node, 0)
        assertEquals(9, result)
    }

    @Test
    fun visitSums() {
        val node = TestDomain.build {
            testSumTriplet(
                one(1),
                two(2, 3),
                three(4, 5, 6)
            ).withMeta(NUMBER_KEY, 7)
        }
        val result = foldingWalker.walkTestSumTriplet(node, 0)
        assertEquals(28, result)
    }

    @Test
    fun visitProductsWithVariadicElements() {
        // No elements, but one meta
        val node1 = TestDomain.build { variadicMin0() }.withMeta(NUMBER_KEY, 1)
        val result1 = foldingWalker.walkVariadicMin0(node1, 0)
        assertEquals(1, result1)

        // One element, and one meta.
        val node2 = TestDomain.build { variadicMin0(42) }.withMeta(NUMBER_KEY, 43)
        val result2 = foldingWalker.walkVariadicMin0(node2, 0)
        assertEquals(85, result2)

        // Four elements and one meta.
        val node3 = TestDomain.build { variadicMin0(1, 2, 3, 4) }.withMeta(NUMBER_KEY, 5)
        val result3 = foldingWalker.walkVariadicMin0(node3, 0)
        assertEquals(15, result3)
    }
}


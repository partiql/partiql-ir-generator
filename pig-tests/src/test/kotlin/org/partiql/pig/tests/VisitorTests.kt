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
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.tests.generated.TestDomain
import kotlin.test.assertEquals

private const val NUMBER_KEY = "number"

class VisitorTests {

    class DummyVisitor : TestDomain.Visitor() {

        // The best way I have found to test this type of visitor is to observe
        // these side effects.  Normally, if you wanted to extract information
        // from a tree you'd use [TestDomain.FoldingVisitor] instead.

        var numberAccumulator: Long = 0
        var stringAccumulator: String = ""

        override fun visitLongPrimitive(node: LongPrimitive) {
            numberAccumulator += node.value
        }

        override fun visitSymbolPrimitive(node: SymbolPrimitive) {
            stringAccumulator += node.text
        }

        override fun visitMetas(metas: MetaContainer) {
            if (metas.containsKey(NUMBER_KEY)) {
                numberAccumulator += metas[NUMBER_KEY] as Int
            }
        }
    }

    @Test
    fun visitProducts() {
        val node = TestDomain.build {
            intPairPair(intPair(1, 2), intPair(3, 4))
        }.withMeta(NUMBER_KEY, 5)

        val visitor = DummyVisitor()
        visitor.walkIntPairPair(node)
        assertEquals(15, visitor.numberAccumulator)
    }

    @Test
    fun visitRecords() {
        val node = TestDomain.build {
            domainLevelRecord(someField = 2, anotherField = "hi", optionalField = 3)
        }.withMeta("number", 4)

        val visitor = DummyVisitor()
        visitor.walkDomainLevelRecord(node)

        assertEquals(9, visitor.numberAccumulator)
        assertEquals("hi", visitor.stringAccumulator)
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

        val visitor = DummyVisitor()
        visitor.walkTestSumTriplet(node)

        assertEquals(28, visitor.numberAccumulator)
    }

    @Test
    fun visitProductsWithVariadicElements() {
        // No elements
        val node1 = TestDomain.build { variadicMin0() }.withMeta(NUMBER_KEY, 1)
        val visitor1 = DummyVisitor()
        visitor1.walkVariadicMin0(node1)
        assertEquals(1, visitor1.numberAccumulator)

        // One element
        val node2 = TestDomain.build { variadicMin0(42) }.withMeta(NUMBER_KEY, 43)
        val visitor2 = DummyVisitor()
        visitor2.walkVariadicMin0(node2)
        assertEquals(85, visitor2.numberAccumulator)

        // Four elements.
        val node3 = TestDomain.build { variadicMin0(1, 2, 3, 4) }.withMeta(NUMBER_KEY, 5)
        val visitor3 = DummyVisitor()
        visitor3.walkVariadicMin0(node3)
        assertEquals(15, visitor3.numberAccumulator)
    }
}

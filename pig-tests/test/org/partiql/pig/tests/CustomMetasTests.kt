package org.partiql.pig.tests

import com.amazon.ionelement.api.MetaContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.pig.tests.generated.TestDomain

/**
 * Demonstrates and tests a custom builder implementation that allocates unique IDs to each node constructed with it.
 */
class CustomMetasTests {

    /** Custom builder implementation that assigns unique Ids to all nodes. */
    class NodeIdAssigningBuilder : TestDomain.Builder {
        private var nextNodeId = 0
        override fun newMetaContainer(): MetaContainer = mapOf("nodeId" to nextNodeId++)
    }

    /**
     * Typically, a builder function such as this should be created which invokes [block] on our custom
     * [TestDomain.Builder] instance.
     *
     * Note that the receiver type is [TestDomain.Builder] and not [NodeIdAssigningBuilder].  Callers should never
     * need to be aware of the specific type of [TestDomain.Builder].
     */
    private fun <T> buildTestDomainWithNodeIds(block: TestDomain.Builder.() -> T) = NodeIdAssigningBuilder().block()

    @Test
    fun testCustomBuilder() {
        val pairPair = buildTestDomainWithNodeIds {
            intPairPair(
                intPair(42, 43),
                intPair(44, 45)
            )
        }

        // Note: nodeIds are allocated in the order the nodes are constructed.
        assertEquals(2, pairPair.metas["nodeId"])
        assertEquals(0, pairPair.first.metas["nodeId"])
        assertEquals(1, pairPair.second.metas["nodeId"])
    }
}


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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.pig.tests.generated.DomainA
import org.partiql.pig.tests.generated.DomainB
import org.partiql.pig.tests.generated.DomainAToDomainBVisitorTransform

/**
 * Test cases for the 5 basic scenarios for domain permutation.
 *
 * test_permute_domain_b is a permutation of test_permute_domain_a.  See test_permute_domain_a and
 * test_permute_domain_b in sample-universe.ion for more details
 *
 * The 5 scenarios are:
 *
 * - A domain-level type has been removed and replace with another type of the same name but with a different
 * definition. Demonstrated with: product_a, record_a and sum_a.
 * - A sum variant is removed (Demonstrated with: sum_b.will_be_removed),
 * - A sum variant is removed and replaced with another of the same name but with a different definition.
 * Demonstrated with: sum_b.will_be_replaced.
 * - A domain-level type has been removed but not replaced in the permuted domain.
 * Demonstrated with: product_to_remove, record_to_remove and sum_to_remove.
 * - A new domain-level type is introduced in the permuted domain. Demonstrated with: new_product, new_record, new_sum.
 *
 * The latter two scenarios are "implicitly" tested.  The simple fact that the generated code for
 * test_permute_domain_b and the [DomainAToDomainBVisitorTransform] compiles is the test.
 */
class PermuteTransformTests {

    class TestTransform : DomainAToDomainBVisitorTransform() {
        override fun transformProductA(node: DomainA.ProductA): DomainB.ProductA =
            DomainB.build {
                productA(node.one.value.toString())
            }


        override fun transformRecordA(node: DomainA.RecordA): DomainB.RecordA =
            DomainB.build {
                recordA(node.one.value.toString())
            }

        override fun transformSumBWillBeRemoved(node: DomainA.SumB.WillBeRemoved): DomainB.SumB =
            DomainB.build {
                willBeUnchanged()
            }


        override fun transformSumBWillBeReplaced(node: DomainA.SumB.WillBeReplaced): DomainB.SumB =
            DomainB.build {
                willBeReplaced(node.something.value.toString())
            }
    }

    private val tt = TestTransform()

    data class TestCase(
        val input: DomainA.DomainANode,
        val expected: DomainB.DomainBNode)

    @ParameterizedTest
    @ArgumentsSource(TestArguments::class)
    fun testPermute(tc: TestCase) {
        // this `when` expression is needed because at this time there is no method we can call
        // on the generated visitor transform that will transform *any* [DomainA.DomainANode].
        // It is possible that one may be added in the future.
        // https://github.com/partiql/partiql-ir-generator/issues/66
        val actual = when(tc.input) {
            is DomainA.ProductA -> tt.transformProductA(tc.input)
            is DomainA.RecordA -> tt.transformRecordA(tc.input)
            is DomainA.SumB -> tt.transformSumB(tc.input)
            // `else` is needed since DomainA.DomainANode is not currently a sealed class
            else -> fail("unexpected type")
        }

        assertEquals(tc.expected, actual)
    }

    class TestArguments : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                DomainA.build {
                    productA(42)
                },
                DomainB.build {
                    productA("42")
                }
            ),
            TestCase(
                DomainA.build {
                    recordA(43)
                },
                DomainB.build {
                    recordA("43")
                }
            ),
            TestCase(
                DomainA.build {
                    willBeRemoved()
                },
                DomainB.build {
                    willBeUnchanged()
                }
            ),
            TestCase(
                DomainA.build {
                    willBeReplaced(44)
                },
                DomainB.build {
                    willBeReplaced("44")
                }
            ),
            TestCase(
                DomainA.build {
                    productA(42)
                },
                DomainB.build {
                    productA("42")
                }
            ),
            TestCase(
                DomainA.build {
                    productA(42)
                },
                DomainB.build {
                    productA("42")
                }
            )
        )
    }
}


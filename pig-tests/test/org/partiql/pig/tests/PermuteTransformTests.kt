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
import org.partiql.pig.tests.generated.TestPermuteDomainA
import org.partiql.pig.tests.generated.TestPermuteDomainAToTestPermuteDomainBVisitorTransform
import org.partiql.pig.tests.generated.TestPermuteDomainB

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
 * test_permute_domain_b and the [TestPermuteDomainAToTestPermuteDomainBVisitorTransform] compiles is the test.
 */
class PermuteTransformTests {

    class TestTransform : TestPermuteDomainAToTestPermuteDomainBVisitorTransform() {
        override fun transformProductA(node: TestPermuteDomainA.ProductA): TestPermuteDomainB.ProductA =
            TestPermuteDomainB.build {
                productA(node.one.value.toString())
            }


        override fun transformRecordA(node: TestPermuteDomainA.RecordA): TestPermuteDomainB.RecordA =
            TestPermuteDomainB.build {
                recordA(node.one.value.toString())
            }

        override fun transformSumBWillBeRemoved(node: TestPermuteDomainA.SumB.WillBeRemoved): TestPermuteDomainB.SumB =
            TestPermuteDomainB.build {
                willBeUnchanged()
            }


        override fun transformSumBWillBeReplaced(node: TestPermuteDomainA.SumB.WillBeReplaced): TestPermuteDomainB.SumB =
            TestPermuteDomainB.build {
                willBeReplaced(node.something.value.toString())
            }
    }

    private val tt = TestTransform()

    data class TestCase(
        val input: TestPermuteDomainA.TestPermuteDomainANode,
        val expected: TestPermuteDomainB.TestPermuteDomainBNode)

    @ParameterizedTest
    @ArgumentsSource(TestArguments::class)
    fun testPermute(tc: TestCase) {
        // this `when` expression is needed because at this time there is no method we can call
        // on the generated visitor transform that will transform *any* [TestPermuteDomainA.TestPermuteDomainANode].
        // It is possible that one may be added in the future.
        // https://github.com/partiql/partiql-ir-generator/issues/66
        val actual = when(tc.input) {
            is TestPermuteDomainA.ProductA -> tt.transformProductA(tc.input)
            is TestPermuteDomainA.RecordA -> tt.transformRecordA(tc.input)
            is TestPermuteDomainA.SumB -> tt.transformSumB(tc.input)

            // `else` is needed since TestPermuteDomainA.TestPermuteDomainANode is not currently a sealed class
            else -> fail("unexpected type")
        }

        assertEquals(tc.expected, actual)
    }

    class TestArguments : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TestCase(
                TestPermuteDomainA.build {
                    productA(42)
                },
                TestPermuteDomainB.build {
                    productA("42")
                }
            ),
            TestCase(
                TestPermuteDomainA.build {
                    recordA(43)
                },
                TestPermuteDomainB.build {
                    recordA("43")
                }
            ),
            TestCase(
                TestPermuteDomainA.build {
                    willBeRemoved()
                },
                TestPermuteDomainB.build {
                    willBeUnchanged()
                }
            ),
            TestCase(
                TestPermuteDomainA.build {
                    willBeReplaced(44)
                },
                TestPermuteDomainB.build {
                    willBeReplaced("44")
                }
            ),
            ///
            TestCase(
                TestPermuteDomainA.build {
                    productA(42)
                },
                TestPermuteDomainB.build {
                    productA("42")
                }
            ),
            TestCase(
                TestPermuteDomainA.build {
                    productA(42)
                },
                TestPermuteDomainB.build {
                    productA("42")
                }
            )
        )
    }
}


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

package org.partiql.pig.domain

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.createIonElementLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.partiql.pig.domain.parser.parseTypeUniverse

class TypeDomainParserTests {
    val em = emptyMetaContainer()
    val loader = createIonElementLoader(IonElementLoaderOptions(includeLocationMeta = true))

    @Test
    fun testProduct() = runTestCase(
        TestCase(
            """
                (universe 
                    (domain test_domain 
                        (product foo (string0 string) (int1 (* int 2))) 
                        (product bar (bat0 bat) (baz1 (? baz)) (blargh2 (* blargh 10)))))
            """,
            """
                (define test_domain 
                    (domain 
                        (product foo string (* int 2))
                        (product bar bat (? baz) (* blargh 10))))
            """))

    @Test
    fun testRecord() = runTestCase(
        TestCase(
            """
                (universe 
                    (domain test_domain 
                        (record foo (bat int) (blorg (? int))) 
                        (record bar (bloo int) (blat (* int 1)))))
            """,
            """
                (define test_domain 
                    (domain 
                        (record foo (bat int) (blorg (? int)))
                        (record bar (bloo int) (blat (* int 1)))))
            """))


    data class TestCase(val expected: String, val input: String)

    @Test
    fun testSum() =
        runTestCase(TestCase(
            """
                (universe 
                    (domain some_domain 
                        (sum test_sum 
                            (product vacuum) 
                            (product lonely (int0 int)) 
                            (product company (int0 int) (symbol1 symbol))
                            (record crowd (first int) (second symbol) (third ion)))))
            """,
            """
                (define some_domain
                    (domain 
                        (sum test_sum
                        // Product with no elements
                        (vacuum)
                        // Product with one element
                        (lonely int)
                        // Product with two elements
                        (company int symbol)
                        // Record with three elements
                        (crowd (first int) (second symbol) (third ion))
                        )))
            """))


    @Test
    fun testPermuteDomain() = runTestCase(
        TestCase(
            """
                (universe 
                    (permute_domain some_domain 
                        (exclude excluded_type) 
                        (include (product included_product (int0 int))) 
                        (with 
                            (permuted_sum altered_sum 
                                (remove removed_variant) 
                                (include (product added_variant (int0 int) (int1 int)))))))
            """,
            """
                (define permuted_domain 
                    (permute_domain some_domain
                        (exclude excluded_type)
                        (include 
                            (product included_product int))
                        (with altered_sum
                            (exclude removed_variant)
                            (include
                                (added_variant int int)))))
            """))

    private fun runTestCase(tc: TestCase) {
        val expected = assertDoesNotThrow("loading expected") {
            loader.loadSingleElement(tc.expected)
        }
        val parsed = assertDoesNotThrow("parsing input") {
            IonReaderBuilder.standard().build(tc.input).use {
                parseTypeUniverse(it)
            }
        }
        assertEquals(expected, parsed.toIonElement())
    }

}


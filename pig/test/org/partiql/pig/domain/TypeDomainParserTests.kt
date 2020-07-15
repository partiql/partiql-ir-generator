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

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.createIonElementLoader
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.partiql.pig.domain.parser.parseTypeUniverse

class TypeDomainParserTests {
    val loader = createIonElementLoader(IonElementLoaderOptions(includeLocationMeta = true))

    @Test
    fun testProduct() = runTestCase(
            """
                (define test_domain 
                    (domain 
                        (product foo (a string) (b (* int 2)))
                        (product bar (a bat) (b (? baz)) (c (* blargh 10)))))
            """)

    @Test
    fun testRecord() = runTestCase(
            """
                (define test_domain 
                    (domain 
                        (record foo (bat int) (blorg (? int)))
                        (record bar (bloo int) (blat (* int 1)))))
            """)

    @Test
    fun testSum() =
        runTestCase(
            """
                (define some_domain
                    (domain 
                        (sum test_sum
                        // Product with no elements
                        (product vacuum)
                        
                        // Product with one element
                        (product lonely (single int))
                        
                        // Product with two elements
                        (product company (id int) (name symbol))
                        
                        // Record with three elements
                        (record crowd (first int) (second symbol) (third ion))
                        )))
            """)


    @Test
    fun testPermuteDomain() = runTestCase(
            """
                (define permuted_domain 
                    (permute_domain some_domain
                        (exclude excluded_type)
                        (include 
                            (product included_product (a int)))
                        (with altered_sum
                            (exclude removed_variant)
                            (include
                                (product added_variant (a int) (b int))))))
            """)

    private fun runTestCase(tc: String) {
        val expected = assertDoesNotThrow("loading the expected type universe") {
            loader.loadSingleElement(tc)
        }
        val parsed = assertDoesNotThrow("parsing type universe") {
            IonReaderBuilder.standard().build(tc).use {
                parseTypeUniverse(it)
            }
        }

        assertEquals(
            ionSexpOf(
                ionSymbol("universe"),
                expected),
            parsed.toIonElement())
    }

}


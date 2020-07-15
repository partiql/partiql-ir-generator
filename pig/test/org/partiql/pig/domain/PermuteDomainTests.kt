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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.model.Arity
import org.partiql.pig.domain.model.DataType
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseTypeUniverse

class PermuteDomainTests {

    /**
     * Runs a simple type universe that uses through all processing steps and verifies the result.
     *
     * Happy path only--no error handling being tested here.
     *
     * TODO:  I believe equality is well implemented now, can this test be simplified?
     * Also, equality for type domain objects is not well implemented which is the reason we do all
     * the assertions instead of comparing test_domain_ex to a parsed example concrete domain.
     */
    @Test
    fun permuteSmokeTest() {
        val typeUniverseWithExtensions = """        
        (define test_domain
            (domain 
                (product pair (first ion) (second ion))
                (product other_pair (a symbol) (b symbol))
                (sum thing
                    (product a (x pair))
                    (product b (y symbol))
                    (product c (z int)))))

        (define permuted_domain 
            (permute_domain test_domain
                (exclude pair)
                (include
                    (product pair (x int) (y int))
                    (product new_pair (z symbol) (n int)))
                (with thing
                    (exclude a)
                    (include
                        (product d (foo pair))
                        (product e (bar symbol))))))
        """

        val td: TypeUniverse = IonReaderBuilder.standard().build(typeUniverseWithExtensions).use { parseTypeUniverse(it) }

        val concretes = td.computeTypeDomains()

        // Fist we perform some basic assertions on the concrete domain
        val concreteDomain = concretes.single { it.tag == "test_domain" }

        // In the original domain, the `pair` type consists of two ions
        val ionPair = concreteDomain.types.single { it.tag == "pair" } as DataType.Tuple
        assertEquals(2, ionPair.namedElements.size)
        assertTrue(ionPair.namedElements.all { it.typeReference.typeName == "ion" && it.typeReference.arity is Arity.Required })

        // In test.domain the "thing" sum has "a", "b", and "c" variants.
        val thing = concreteDomain.types.single { it.tag == "thing" } as DataType.Sum
        assertEquals(3, thing.variants.size)
        assertEquals(1, thing.variants.filter { it.tag == "a"}.size)
        assertEquals(1, thing.variants.filter { it.tag == "b"}.size)
        assertEquals(1, thing.variants.filter { it.tag == "c"}.size)

        // Then we verify the permuted domain
        val permutedDomain = concretes.single { it.tag == "permuted_domain" }
        // Permute domain still has 3 types of test_domain + 1 more
        assertEquals(4, permutedDomain.userTypes.size)
        assertTrue(permutedDomain.types.map { it.tag }.containsAll(listOf("pair", "thing", "other_pair", "new_pair")))

        // In the permuted domain, the 'pair' type consists of two ints'
        val intPair = permutedDomain.types.single { it.tag == "pair" } as DataType.Tuple
        assertEquals(2, intPair.namedElements.size)
        assertTrue(intPair.namedElements.all { it.typeReference.typeName == "int" && it.typeReference.arity is Arity.Required })

        // In the permuted domain, the "thing.a" variant has been replaced with "thing.d" and "thing.e" has been added
        val exThing = permutedDomain.types.single { it.tag == "thing" } as DataType.Sum
        assertEquals(4, exThing.variants.size)
        assertTrue(exThing.variants.none { it.tag == "a"})
        assertEquals(1, exThing.variants.filter { it.tag == "b"}.size)
        assertEquals(1, exThing.variants.filter { it.tag == "c"}.size)
        assertEquals(1, exThing.variants.filter { it.tag == "d"}.size)
        assertEquals(1, exThing.variants.filter { it.tag == "e"}.size)
    }
}

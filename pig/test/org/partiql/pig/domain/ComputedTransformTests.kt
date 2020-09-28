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
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.model.TypeUniverse
import org.partiql.pig.domain.parser.parseTypeUniverse

class ComputedTransformTests {
    private val typeUniverseWithExtensions = """        
        (define test_domain
            (domain 
                (product pair first::ion second::ion)
                (product other_pair first::symbol second::symbol)
                (sum thing
                    (a x::pair)
                    (b y::symbol)
                    (c z::int))))

        (define permuted_domain 
            (permute_domain test_domain
                (exclude pair)
                (include
                    (product pair n::int t::int)
                    (product new_pair m::symbol n::int))
                (with thing
                    (exclude a)
                    (include
                        (d a::pair)
                        (e b::symbol)))))
        """

    @Test
    fun foo() {
        val td: TypeUniverse = IonReaderBuilder.standard().build(typeUniverseWithExtensions).use { parseTypeUniverse(it) }

        val concretes = td.computeTypeDomains()

        val original = concretes.single { it.tag == "test_domain" }
        val permuted = concretes.single { it.tag == "permuted_domain" }

        println(original.toIonElement())
        println(permuted.toIonElement())

        val x = original.computeTransform(permuted)
        println(x.toIonElement())
    }
}
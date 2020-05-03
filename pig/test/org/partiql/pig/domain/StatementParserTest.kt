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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.model.*
import org.partiql.pig.domain.parser.parseTypeUniverse

class StatementParserTest {
    val em = emptyMetaContainer()

    @Test
    fun testProduct() {
        val vector = """
            (define test_domain 
                (domain 
                    (product foo string (* int 2))
                    (product bar bat (? baz) (* blargh 10))))
        """

        val universe = IonReaderBuilder.standard().build(vector).use { parseTypeUniverse(it) }
        assertEquals(1, universe.statements.size)
        val typeDomain = universe.statements.first() as TypeDomain

        assertEquals("test_domain", typeDomain.name)
        assertEquals(2, typeDomain.types.filter { !it.isBuiltin }.size)

        val actualFoo = typeDomain.types.single { it.tag == "foo" }
        val expectedFoo =
            DataType.Tuple(
                tag = "foo",
                tupleType = TupleType.PRODUCT,
                namedElements = listOf(
                    NamedElement("string0", TypeRef("string", Arity.Required, em), em),
                    NamedElement("int1", TypeRef("int", Arity.Variadic(2), em), em)),
                metas = em)

        assertEquals(expectedFoo, actualFoo, "foo product must match expected")

        val actualBar = typeDomain.types.single { it.tag == "bar" }
        val expectedBar =
            DataType.Tuple(
                tag = "bar",
                tupleType = TupleType.PRODUCT,
                namedElements = listOf(
                    NamedElement("bat0", TypeRef("bat", Arity.Required, em), em),
                    NamedElement("baz1", TypeRef("baz", Arity.Optional, em), em),
                    NamedElement("blargh2", TypeRef("blargh", Arity.Variadic(10), em), em)),
                metas = em)

        assertEquals(expectedBar, actualBar, "bar product must match expected")
    }

    @Test
    fun testRecord() {
        val vector = """
            (define test_domain 
                (domain 
                    (product foo (bat int) (blorg (? int)))
                    (product bar (bloo int) (blat (* int 1)))))
        """

        val td = IonReaderBuilder.standard().build(vector).use { parseTypeUniverse(it) }

        assertEquals(
            TypeUniverse(
                listOf(
                    TypeDomain(
                        name = "test_domain",
                        userTypes = listOf(
                            DataType.Tuple(
                                tag = "foo",
                                tupleType = TupleType.RECORD,
                                namedElements = listOf(
                                    NamedElement("bat", TypeRef("int", Arity.Required, em), em),
                                    NamedElement("blorg", TypeRef("int", Arity.Optional, em), em)),
                                metas = em),
                            DataType.Tuple(
                                tag = "bar",
                                tupleType = TupleType.RECORD,
                                namedElements = listOf(
                                    NamedElement("bloo", TypeRef("int", Arity.Required, em), em),
                                    NamedElement("blat", TypeRef("int", Arity.Variadic(1), em), em)),
                                metas = em))))),
            td)
    }

    @Test
    fun testSum() {
        val vector = """
            (define some_domain
                (domain 
                    (sum case_sensitivity   
                        (case_sensitive)
                        (case_insensitive))))
        """

        val td = IonReaderBuilder.standard().build(vector).use { parseTypeUniverse(it) }

        assertEquals(
            TypeUniverse(
                listOf(
                    TypeDomain(
                        "some_domain",
                        listOf(
                            DataType.Sum(
                                "case_sensitivity",
                                listOf(
                                    DataType.Tuple("case_sensitive", TupleType.PRODUCT, listOf(), em),
                                    DataType.Tuple("case_insensitive", TupleType.PRODUCT, listOf(), em)),
                                em)),
                        em))),
            td)
    }

    @Test
    fun testPermuteDomain() {
        val vector = """
            (define permuted_domain 
                (permute_domain some_domain
                    (exclude excluded_type)
                    (include 
                        (product included_product int))
                    (with altered_sum
                        (exclude removed_variant)
                        (include
                            (added_variant int int)))))
        """

        val actualTypeUniverse = IonReaderBuilder.standard().build(vector).use { parseTypeUniverse(it) }
        val expectedTypeUniverse = TypeUniverse(
            listOf(
                PermutedDomain(
                    name = "permuted_domain",
                    permutesDomain = "some_domain",
                    excludedTypes = listOf("excluded_type"),
                    includedTypes = listOf<DataType>(
                        DataType.Tuple(
                            tag = "included_product",
                            tupleType = TupleType.PRODUCT,
                            namedElements = listOf(
                                NamedElement("int0", TypeRef("int", Arity.Required, em), em)),
                            metas = em)),
                    permutedSums = listOf(
                        PermutedSum(
                            tag = "altered_sum",
                            removedVariants = listOf("removed_variant"),
                            addedVariants = listOf(
                                DataType.Tuple(
                                    tag = "added_variant",
                                    tupleType = TupleType.PRODUCT,
                                    namedElements = listOf(
                                        NamedElement("int0", TypeRef("int", Arity.Required, em), em),
                                        NamedElement("int1", TypeRef("int", Arity.Required, em), em)),
                                    metas = em)),
                            metas = em)),
                    metas = em)))

        // If this assertion is failing, place a breakpoint on the following line and
        // inspect the type universes in the debugger.
        assertEquals(expectedTypeUniverse, actualTypeUniverse)
    }
}


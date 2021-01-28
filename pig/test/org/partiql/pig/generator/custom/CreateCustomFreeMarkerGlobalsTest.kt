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

package org.partiql.pig.generator.custom

import com.amazon.ionelement.api.emptyMetaContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.model.*
import java.time.OffsetDateTime

class CreateCustomFreeMarkerGlobalsTest {
    val em = emptyMetaContainer()

    /**
     * Transforms a basic [TypeUniverse] instance to [CustomFreeMarkerGlobals] and asserts that the result is as
     * expected.
     */
    @Test
    fun smokeTest() {
        val inputTypeUniverse =
            TypeUniverse(
                listOf(
                    TypeDomain(
                        tag = "test_domain",
                        userTypes = listOf(
                            DataType.UserType.Tuple(
                                tag = "foo",
                                tupleType = TupleType.PRODUCT,
                                namedElements = listOf(
                                    NamedElement("bat", "baz", TypeRef("int", Arity.Required, em), em)),
                                metas = em),
                            DataType.UserType.Sum(
                                "some_sum",
                                variants = listOf(
                                    DataType.UserType.Tuple(
                                        tag = "bar",
                                        tupleType = TupleType.PRODUCT,
                                        namedElements = listOf(
                                            NamedElement("bloo", "blar", TypeRef("int", Arity.Required, em), em)),
                                        metas = em)),
                                metas = em)))))

        val actualFreemarkerGlobals = createCustomFreeMarkerGlobals(inputTypeUniverse.computeTypeDomains()).copy(
            generatedDate = OffsetDateTime.MAX // For the assertion below
        )

        val expectedFreeMarkerGlobals =
            CustomFreeMarkerGlobals(
                domains =
                listOf(
                    CTypeDomain(
                        name = "test_domain",
                        tuples = listOf(
                            CTuple(
                                tag = "foo",
                                tupleType = TupleType.PRODUCT,
                                elements = listOf(
                                    CElement(
                                        identifier = "bat",
                                        tag = "baz",
                                        type = "int",
                                        isVariadic = false,
                                        isOptional = false
                                    )),
                                arity = IntRange(1, 1),
                                memberOfType = null)),
                        sums = listOf(
                            CSum(
                                name = "some_sum",
                                variants= listOf(
                                    CTuple(
                                        tag ="bar",
                                        memberOfType="some_sum",
                                        elements=listOf(
                                                CElement(
                                                    identifier="bloo",
                                                    tag="blar",
                                                    type="int",
                                                    isVariadic=false,
                                                    isOptional=false)),
                                        arity=1..1,
                                        tupleType=TupleType.PRODUCT)))))),
                generatedDate = OffsetDateTime.MAX)


        assertEquals(expectedFreeMarkerGlobals, actualFreemarkerGlobals)
    }
}
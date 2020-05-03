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

import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ion.IonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.pig.tests.generated.partiql_basic
import org.partiql.pig.tests.generated.toy_lang

class SmokeTests {

    @Test
    fun toy_lang_test() {
        val node = toy_lang.build {
            plus(variable("foo"), lit(ionInt(42)), lit(ionNull(IonType.STRING)))
        }

        val expectedIonElement = node.toIonElement()

        val roundTrippedNode = toy_lang.transform(expectedIonElement)

        val roundTrippedElement = roundTrippedNode.toIonElement()
        assertEquals(expectedIonElement, roundTrippedElement)
    }

    @Test
    fun partiql_basic_test() {
        val node = partiql_basic.build {
            select(
                all(),
                project_list(
                    project_expr(
                        plus(
                            lit(ionInt(1)),
                            lit(ionInt(41))),
                        "select_alias")),
                scan(
                    id("foo", case_sensitive(), unqualified()),
                    "as_foo",
                    "at_foo",
                    "by_foo"))
        }

        val expectedIiv = node.toIonElement()

        val roundTrippedNode = partiql_basic.transform(expectedIiv)

        val roundTrippedIiv = roundTrippedNode.toIonElement()

        assertEquals(expectedIiv, roundTrippedIiv)
    }
}



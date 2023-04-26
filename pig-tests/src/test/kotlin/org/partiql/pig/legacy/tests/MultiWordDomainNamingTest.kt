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

package org.partiql.pig.legacy.tests

import org.junit.jupiter.api.Test
import org.partiql.pig.legacy.tests.generated.MultiWordDomain
import kotlin.test.assertEquals

class MultiWordDomainNamingTest {

    // The main check here is if the conversion from snake_case to PascalCase is happening correctly
    // for all of generated identifiers.  The way its tested is by the compilation fo this file,
    // but we include some assertions anyway just to validate our assumptions further.
    @Test
    @Suppress("ComplexRedundantLet")
    fun compilesCorrectly() {
        // MultiWordDomain products

        MultiWordDomain.build { aaaAaa() }

        MultiWordDomain.build {
            aaaAab(dField = 123)
        }.let { a: MultiWordDomain.AaaAab ->
            assertEquals(123, a.dField?.value)
        }

        MultiWordDomain.build { aaaAac(dField = 123, eField = "foo") }.let { a: MultiWordDomain.AaaAac ->
            assertEquals(123, a.dField?.value)
            assertEquals("foo", a.eField?.text)
        }

        MultiWordDomain.build {
            aaaAad(dField = listOf(1L, 2L, 3L))
        }.let { a: MultiWordDomain.AaaAad ->
            assertEquals(listOf(1L, 2L, 3L), a.dField.map { i -> i.value })
        }

        MultiWordDomain.build {
            aaaAae(dField0 = 1, dField1 = 2)
        }.let { a: MultiWordDomain.AaaAae ->
            assertEquals(listOf(1L, 2L), a.dField.map { i -> i.value })
        }

        // SssTtt variants
        MultiWordDomain.build {
            lll(uField = 1)
        }.let { a: MultiWordDomain.SssTtt.Lll ->
            assertEquals(1L, a.uField.value)
        }

        MultiWordDomain.build {
            mmm(vField = "bleeg")
        }.let { a: MultiWordDomain.SssTtt.Mmm ->
            assertEquals("bleeg", a.vField.text)
        }

        // Record
        MultiWordDomain.build {
            rrr(aField = 123, bbbField = 456)
        }.let { a: MultiWordDomain.Rrr ->
            // aField uses the default name which is the same as its tag.
            assertEquals(123L, a.aField.value)
            // bbbField is the non-default name which is different than the element's tag.
            assertEquals(456L, a.bbbField.value)
        }
    }
}

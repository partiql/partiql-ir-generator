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

import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.createIonElementLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.runtime.MalformedDomainDataException
import org.partiql.pig.tests.generated.partiql_basic

class IonElementTransformerErrorTests {
    data class ErrorTestCase(val domainText: String, val message: String)

    @ParameterizedTest
    @MethodSource("parametersForSerializationErrorTest")
    fun serializationErrorTest(tc: ErrorTestCase) {
        val element = createIonElementLoader(IonElementLoaderOptions(includeLocationMeta = true)).loadSingleElement(tc.domainText)
        val ex = assertThrows<MalformedDomainDataException> { partiql_basic.transform(element) }
        assertEquals(tc.message, ex.message, "Exception's message must match")
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForSerializationErrorTest() = listOf(
            // Empty s-exp
            ErrorTestCase(
                "()",
                "1:1: Cannot get head of empty container"),
            // Undefined tag
            ErrorTestCase(
                "(asdfasdf)",
                "1:2: Unknown tag 'asdfasdf' for domain 'partiql_basic'"),
            // Invalid arity (too few)
            ErrorTestCase(
                "(lit)",
                "1:1: 1..1 argument(s) were required to `lit`, but 0 was/were supplied."),
            // Invalid arity (too many)
            ErrorTestCase(
                "(lit 1 2)",
                "1:1: 1..1 argument(s) were required to `lit`, but 2 was/were supplied."),
            // Incorrect type of second argument to plus
            ErrorTestCase(
                "(plus (lit 1) (project_value (lit 1)))",
                "1:15: Expected 'class org.partiql.pig.tests.generated.partiql_basic${'$'}expr' but found 'class org.partiql.pig.tests.generated.partiql_basic${'$'}projection${'$'}project_value'"),
            // Missing record field
            ErrorTestCase(
                "(select (from (scan (lit foo))))",
                "1:1: Required field 'project' was not found within 'select' record"),
            // Undefined record field
            ErrorTestCase(
                "(select (extra_field 1) (project (project_value (lit 1))) (from (scan (lit foo) null null null)))",
                "1:1: Unexpected field 'extra_field' encountered")
        )
    }
}
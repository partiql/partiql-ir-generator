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

import com.amazon.ionelement.api.IonElectrolyteException
import com.amazon.ionelement.api.ElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.domain.parser.ParserErrorContext
import org.partiql.pig.domain.parser.parseTypeUniverse
import org.partiql.pig.errors.PigError

class TypeDomainParserErrorsTest {

    // TODO: aren't there more permute_domain related errors?

    @ParameterizedTest
    @MethodSource("parametersForErrorsTest")
    fun errorsTest(tc: TestCase) {
        val ex = assertThrows<PigException> {
            val oops = parseTypeUniverse(tc.typeUniverseText)
            println("this was erroneously parsed: ${oops.toIonElement()}")
        }
        assertEquals(tc.expectedError, ex.error)

        // This is mainly to improve code coverage but also ensures the
        // message formatting function doesn't throw
        assertNotNull(tc.expectedError.message)
    }

    companion object {
        data class TestCase(val typeUniverseText: String, val expectedError: PigError)

        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorsTest() = listOf(
            TestCase(
                "(", // note:  ParserErrorContext.IonElementError.equals doesn't check the exception
                makeErr( ParserErrorContext.IonElementError(IonElectrolyteException(null, "")))),

            TestCase(
                "(bad_tag)",
                makeErr(1, 2, ParserErrorContext.InvalidTopLevelTag("bad_tag"))),

            TestCase(
                "(define huh hee hoo)",
                makeErr(1, 1, ParserErrorContext.InvalidArityForTag(2..2, "define", 3))),

            TestCase(
                "(define huh (invalid_constructor))",
                makeErr(1, 14, ParserErrorContext.UnknownConstructor("invalid_constructor"))),

            TestCase(
                "(define huh (domain (bad_tag)))",
                makeErr(1, 22, ParserErrorContext.InvalidDomainLevelTag("bad_tag"))),

            TestCase(
                "(define huh (domain (product huh int (bad_tag))))",
                makeErr(1, 39, ParserErrorContext.ExpectedTypeReferenceArityTag("bad_tag"))),

            TestCase(
                "(define huh (permute_domain huh (bad_tag)))",
                makeErr(1, 33, ParserErrorContext.InvalidPermutedDomainTag("bad_tag"))),

            TestCase(
                "(define huh (permute_domain huh (with huh (bad_tag))))",
                makeErr(1, 43, ParserErrorContext.InvalidWithSumTag("bad_tag"))),

            TestCase(
                "(define huh (domain (product huh (? ))))",
                makeErr(1, 34, ParserErrorContext.InvalidArityForTag(IntRange(1, 1), "?", 0))),

            TestCase(
                "(define huh (domain (product huh (? one two))))",
                makeErr(1, 34, ParserErrorContext.InvalidArityForTag(IntRange(1, 1), "?", 2))),

            TestCase(
                "(define huh (domain (record foo (name_field_with_too_few_elements))))",
                makeErr(1, 33, ParserErrorContext.InvalidArity(2, 1))),

            TestCase(
                "(define huh (domain (record foo (name_field_with_too many elements))))",
                makeErr(1, 33, ParserErrorContext.InvalidArity(2, 3))),

            TestCase( // Covers first place in parser this can be thrown
                "(define huh (domain (product huh 42)))",
                makeErr(1, 34, ParserErrorContext.ExpectedSymbolOrSexp(ElementType.INT))),

            TestCase( // Covers second place in parser this can be thrown
                "(define huh (domain (product huh int 42)))",
                makeErr(1, 38, ParserErrorContext.ExpectedSymbolOrSexp(ElementType.INT)))
        )
    }
}



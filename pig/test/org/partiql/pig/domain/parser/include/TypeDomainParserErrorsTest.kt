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

package org.partiql.pig.domain.parser.include

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementLoaderException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.domain.makeErr
import org.partiql.pig.domain.parser.ParserErrorContext
import org.partiql.pig.domain.toIonElement
import org.partiql.pig.errors.PigError
import org.partiql.pig.errors.PigException
import org.partiql.pig.util.makeFakePath
import org.partiql.pig.util.parseTypeUniverseString

class TypeDomainParserErrorsTest {

    // TODO: aren't there more permute_domain related errors?

    @ParameterizedTest
    @MethodSource("parametersForErrorsTest")
    fun errorsTest(tc: TestCase) {
        val ex = assertThrows<PigException> {
            val oops = parseTypeUniverseString(tc.typeUniverseText)
            println("this was erroneously parsed: ${oops.toIonElement()}")
        }
        assertEquals(tc.expectedError, ex.error)

        // This is mainly to improve code coverage but also ensures the
        // message formatting function doesn't throw
        assertNotNull(tc.expectedError.toString())
    }

    companion object {
        data class TestCase(val typeUniverseText: String, val expectedError: PigError)

        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorsTest() = listOf(
            TestCase(
                "(", // note:  ParserErrorContext.IonElementError.equals doesn't check the exception
                makeErr( ParserErrorContext.IonElementError(IonElementLoaderException(null, "")))),

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
                "(define huh (domain (product one::huh two::int three::(bad_tag))))",
                makeErr(1, 56, ParserErrorContext.ExpectedTypeReferenceArityTag("bad_tag"))),

            TestCase(
                "(define huh (permute_domain huh (bad_tag)))",
                makeErr(1, 33, ParserErrorContext.InvalidPermutedDomainTag("bad_tag"))),

            TestCase(
                "(define huh (permute_domain huh (with huh (bad_tag))))",
                makeErr(1, 43, ParserErrorContext.InvalidWithSumTag("bad_tag"))),

            TestCase(
                "(define huh (domain (product x::huh y::(? ))))",
                makeErr(1, 37, ParserErrorContext.InvalidArityForTag(IntRange(1, 1), "?", 0))),

            TestCase(
                "(define huh (domain (product huh x::(? o::one t::two))))",
                makeErr(1, 34, ParserErrorContext.InvalidArityForTag(IntRange(1, 1), "?", 2))),

            TestCase(
                "(define huh (domain (record foo (name_field_with_too_few_elements))))",
                makeErr(1, 33, ParserErrorContext.InvalidArity(2, 1))),

            TestCase(
                "(define huh (domain (record foo (name_field_with_too many elements))))",
                makeErr(1, 33, ParserErrorContext.InvalidArity(2, 3))),

            TestCase( // Covers first place in parser this can be thrown
                "(define huh (domain (product huh x::42)))",
                makeErr(1, 34, ParserErrorContext.ExpectedSymbolOrSexp(ElementType.INT))),

            TestCase( // Covers second place in parser this can be thrown
                "(define huh (domain (product huh x::int y::42)))",
                makeErr(1, 41, ParserErrorContext.ExpectedSymbolOrSexp(ElementType.INT))),

            TestCase(
                "(include_file \"some-non-existing-file.ion\")",
                makeErr(1, 1,
                    ParserErrorContext.IncludeFileNotFound(
                        "some-non-existing-file.ion",
                        listOf(makeFakePath("some-non-existing-file.ion"))
                ))),
            TestCase(
                "(include_file \"some-sub-dir/some-non-existing-file.ion\")",
                makeErr(1, 1,
                    ParserErrorContext.IncludeFileNotFound(
                        "some-sub-dir/some-non-existing-file.ion",
                        listOf(makeFakePath("some-sub-dir/some-non-existing-file.ion"))
                ))),
            TestCase(
                "(include_file \"../doesntmatter.ion\")",
                makeErr(1, 15, ParserErrorContext.IncludeFilePathContainsParentDirectory)),
            TestCase(
                "(include_file \"c:/windows/drive/letter/is/bad.ion\")",
                makeErr(1, 15, ParserErrorContext.IncludeFilePathContainsIllegalCharacter(':'))),
            TestCase(
                """(include_file "\\windows\\path\\separator")""",
                makeErr(1, 15, ParserErrorContext.IncludeFilePathContainsIllegalCharacter('\\'))),
            TestCase(
                "(include_file \"space in name\")",
                makeErr(1, 15, ParserErrorContext.IncludeFilePathContainsIllegalCharacter(' ')))
        )
    }
}




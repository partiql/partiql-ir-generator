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

package org.partiql.pig.domain.parser


//import com.amazon.ionelement.api.Ion
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import org.partiql.pig.domain.PigException
import org.partiql.pig.errors.ErrorContext
import org.partiql.pig.errors.PigError

/**
 * Variants of [ParserErrorContext] contain details about various parse errors that can be encountered
 * during type universe parsing.
 *
 * They each variant is a data class
 */
sealed class ParserErrorContext(val msgFormatter: () -> String): ErrorContext {
    override val message: String get() = msgFormatter()

    /** Indicates that an []IonElectrolyteException] was thrown during parsing of a type universe. */
    data class IonElementError(val ex: IonElementException)
        : ParserErrorContext({ ex.message!! }) {
        // This is for unit tests... we don't include IonElectrolyteException here since it doesn't implement
        // equals anyway
        override fun equals(other: Any?): Boolean = other is IonElementError
        override fun hashCode(): Int = 0
    }

    data class UnknownConstructor(val tag: String)
        : ParserErrorContext({ "Unknown constructor: '$tag' (expected constructors are 'domain' or 'permute_domain')" })

    data class InvalidDomainLevelTag(val tag: String)
        : ParserErrorContext({ "Invalid domain-level tag: '$tag'"})

    data class InvalidTopLevelTag(val tag: String)
        : ParserErrorContext({ "Invalid top-level tag: '$tag'"})

    data class InvalidPermutedDomainTag(val tag: String)
        : ParserErrorContext({ "Invalid tag for permute_domain body: '$tag'"})

    data class InvalidWithSumTag(val tag: String)
        : ParserErrorContext({ "Invalid tag for with body: '$tag'"})

    data class ExpectedTypeReferenceArityTag(val tag: String)
        : ParserErrorContext({ "Expected '*' or '?' but found '$tag'"})

    data class ExpectedSymbolOrSexp(val foundType: ElementType)
        : ParserErrorContext({ "Expected a symbol or s-exp but encountered a value of type $foundType"})

    data class InvalidArity(val expectedCount: Int, val actualCount: Int)
        : ParserErrorContext({ "$expectedCount argument(s) were required here, but $actualCount was/were supplied."})

    data class InvalidArityForTag(val expectedCount: IntRange, val tag: String, val actualCount: Int)
        : ParserErrorContext({ "$expectedCount argument(s) were required to '$tag', but $actualCount was/were supplied."})
}


fun parseError(blame: IonLocation?, context: ErrorContext): Nothing =
    PigError(blame, context).let {
        throw when (context) {
            is ParserErrorContext.IonElementError -> {
                // Include cause in stack trace for debuggability
                PigException(it, context.ex)
            }
            else -> PigException(it)
        }
    }

fun parseError(blame: IonElement, context: ErrorContext): Nothing {
    val loc = blame.metas.location
    parseError(loc, context)
}


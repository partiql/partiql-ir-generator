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

package org.partiql.pig.domain.model

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.pig.domain.PigException

data class TypeUniverse(val statements: List<Statement>) {

    /**
     * Returns a list of the original [TypeDomain] instances and the [TypeDomain]s computed from [PermutedDomain]
     * instances, performing detailed semantic checking along the way.
     *
     * @throws [PigException] when the first semantic error is encountered.
     */
    fun computeTypeDomains(): List<TypeDomain> {
        if(statements.none()) {
            semanticError(null, SemanticErrorContext.EmptyUniverse)
        }

        val domains = mutableMapOf<String, TypeDomain>()
        statements.forEach { stmt ->
            val typeDomain = when(stmt) {
                is TypeDomain -> stmt
                is PermutedDomain -> {
                    // Note that we compute the [TypeDomain] for the [PermutedDomain] *before* semantic checking.
                    stmt.computePermutation(domains)
                }
            }

            typeDomain.checkSemantics()

            if(domains.putIfAbsent(typeDomain.name, typeDomain) != null) {
                semanticError(typeDomain.metas, SemanticErrorContext.DuplicateTypeDomainName(typeDomain.name))
            }
        }

        return domains.values.toList()
    }

    /**
     * Generates an s-expression representation of this [TypeUniverse].
     *
     * This primarily aids in unit testing and is not intended to have an identical structure to PIG-s type universe
     * syntax.
     */
    fun toIonElement(): IonElement =
        ionSexpOf(
            ionSymbol("universe"),
            *statements.map { it.toIonElement() }.toTypedArray())
}


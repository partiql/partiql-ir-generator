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

package org.partiql.pig.runtime

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.location

abstract class IonElementTransformerBase<T : DomainNode> {
    fun transform(maybeSexp: SexpElement): T =
        try {
            innerTransform(maybeSexp)
        } catch (ex: IonElementException) {
            throw MalformedDomainDataException(ex.location, ex.description, ex)
        }

    protected inline fun <reified R : T> AnyElement.transformExpect(): R {
        val domainObject = innerTransform(this.asSexp())
        return (domainObject as? R) ?: errMalformed(
            this.metas.location,
            "Expected '${R::class.java}' but found '${domainObject.javaClass}'"
        )
    }

    protected abstract fun innerTransform(sexp: SexpElement): T
}

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

import com.amazon.ionelement.api.IonElectrolyteException
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.location

abstract class IonElementTransformerBase<T: DomainNode> {
    fun transform(maybeSexp: IonElement): T =
        try {
            innerTransform(maybeSexp)
        } catch(ex: IonElectrolyteException) {
            throw MalformedDomainDataException(ex.location, ex.description, ex)
        }

    protected inline fun <reified R: T> IonElement.transformExpect(): R {
        val domainObject = innerTransform(this)
        return (domainObject as? R) ?: errMalformed(
            this.metas.location,
            "Expected '${R::class.java}' but found '${domainObject.javaClass}'")
    }

    protected abstract fun innerTransform(maybeSexp: IonElement): T
}

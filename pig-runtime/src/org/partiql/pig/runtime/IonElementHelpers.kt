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

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol


/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun IonElement.toIonElement() = this

/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun String?.toIonElement() = ionSymbol(this)

/**
 * This seemingly useless function reduces complexity of the Kotlin template by providing a consistent way to
 * convert (or not) a variable to an [IonElement].  This has the same signature as the other `.toIonElement()`
 * extensions in package.
 */
fun Long.toIonElement() = ionInt(this)




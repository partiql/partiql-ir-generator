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

package org.partiql.pig.errors

import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.locationToString

/**
 * [ErrorContext] instances provide information about an error message which can later be used
 * to produce human readable error message.
 *
 * [ErrorContext] instances must implement [toString] to produce the human readable text,
 * and [equals]
 */
interface ErrorContext {
    val message: String
}

data class PigError(val location: IonLocation?, val context: ErrorContext) {
    override fun toString(): String = "${locationToString(location)}: ${context.message}"
}

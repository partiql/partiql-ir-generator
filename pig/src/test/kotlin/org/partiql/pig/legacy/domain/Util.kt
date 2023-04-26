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

package org.partiql.pig.legacy.domain

import com.amazon.ionelement.api.IonTextLocation
import org.partiql.pig.legacy.errors.ErrorContext
import org.partiql.pig.legacy.errors.PigError

fun makeErr(line: Int, col: Int, errorContext: ErrorContext) =
    PigError(IonTextLocation(line.toLong(), col.toLong()), errorContext)

fun makeErr(errorContext: ErrorContext) =
    PigError(null, errorContext)

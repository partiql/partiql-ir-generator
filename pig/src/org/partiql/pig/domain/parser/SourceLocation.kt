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

import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.MetaContainer

/**
 * Used to construct helpful error messages for the end-user, who will be able to the location of a given
 * error which includes the file name, line & column.
 */
data class SourceLocation(val path: String, val location: IonLocation) {
    override fun toString(): String {
        return "$path:$location"
    }
}

internal const val SOURCE_LOCATION_META_TAG = "\$pig_source_location"

internal val MetaContainer.sourceLocation
    get() = this[SOURCE_LOCATION_META_TAG] as? SourceLocation

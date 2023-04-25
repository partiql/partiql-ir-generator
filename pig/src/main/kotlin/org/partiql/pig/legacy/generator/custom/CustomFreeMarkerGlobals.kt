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

package org.partiql.pig.legacy.generator.custom

import java.time.OffsetDateTime

/** The properties of [CustomFreeMarkerGlobals] become global variables for our freemarker templates. */
@Suppress("unused")
data class CustomFreeMarkerGlobals(
    val domains: List<CTypeDomain>,
    val generatedDate: OffsetDateTime
)

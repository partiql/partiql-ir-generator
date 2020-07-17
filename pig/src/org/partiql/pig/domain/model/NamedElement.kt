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

import com.amazon.ionelement.api.MetaContainer

/** An element of a product or record. */
class NamedElement(
    /** The name of the element that should be used in generated code. */
    val identifier: String,
    /** The tag used in the s-expression representation, if this is a record element. */
    val tag: String,
    /** A reference to the type of this element.*/
    val typeReference: TypeRef,
    val metas: MetaContainer
)
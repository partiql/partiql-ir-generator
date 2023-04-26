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

package org.partiql.pig.legacy.cmdline

import java.io.File

sealed class TargetLanguage {
    abstract val domains: Set<String>?

    data class Kotlin(val namespace: String, val outputDirectory: File, override val domains: Set<String>? = null) : TargetLanguage()
    data class Custom(val templateFile: File, val outputFile: File, override val domains: Set<String>? = null) : TargetLanguage()
    data class Html(val outputFile: File, override val domains: Set<String>? = null) : TargetLanguage()
    data class Ion(val outputFile: File, override val domains: Set<String>? = null) : TargetLanguage()
}

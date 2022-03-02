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

package org.partiql.pig.generator

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import freemarker.template.TemplateNumberModel
import java.io.IOException
import java.io.StringWriter

/**
 * This was shamelessly copied from https://stackoverflow.com/questions/1235179/simple-way-to-repeat-a-string-in-java
 * and converted to Kotlin with IntelliJ.
 */
class IndentDirective : TemplateDirectiveModel {

    @Throws(TemplateException::class, IOException::class)
    override fun execute(
        environment: Environment,
        parameters: Map<*, *>,
        templateModels: Array<TemplateModel>,
        body: TemplateDirectiveBody
    ) {
        var count: Int? = null
        val iterator = parameters.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val name = entry.key as String
            val value = entry.value as TemplateModel

            if (name == COUNT == true) {
                if (value is TemplateNumberModel == false) {
                    throw TemplateModelException("The \"$COUNT\" parameter must be a number")
                }
                count = value.asNumber.toInt()
                if (count < 0) {
                    throw TemplateModelException("The \"$COUNT\" parameter cannot be negative")
                }
            } else {
                throw TemplateModelException("Unsupported parameter '$name'")
            }
        }
        if (count == null) {
            throw TemplateModelException("The required \"$COUNT\" parameteris missing")
        }

        val indentation = " ".repeat(count)
        val writer = StringWriter()
        body.render(writer)
        val string = writer.toString()
        val lineFeed = "\n"
        val containsLineFeed = string.contains(lineFeed) == true
        val tokens = string.split(lineFeed.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (token in tokens) {
            environment.out.write(indentation + token + if (containsLineFeed == true) lineFeed else "")
        }
    }

    companion object {

        private val COUNT = "count"
    }
}

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
import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.head
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.tail

/**
 * Contains an "intermediate" representation of a PIG record.
 *
 * This class was intended only for use by PIG-generated domain serialization code. Human written client code
 * should avoid using this class.
 *
 * Instances are single use only and should be discarded after extracting all field values with [processRequiredField]
 * or [processOptionalField].
 */
class IntermediateRecord(
    /** The tag of the record. Used for error reporting purposes only. */
    private val recordTagName: String,
    /** The location of the record within the data being transformer. */
    private val location: IonLocation?,
    /** The fields and their values. */
    fields: Map<String, List<AnyElement>>
) {
    private val fieldMap = fields.toMutableMap()

    /**
     * If a field of named [fieldName] exists in [fieldMap], removes it from [fieldMap] and passes its value to
     * [deserFunc] to perform deserialization.  Returns `null` if the field does not exist in [fieldMap].
     */
    fun <T> processOptionalField(fieldName: String, deserFunc: (AnyElement) -> T): T? =
        fieldMap.remove(fieldName)?.let { values: List<AnyElement> ->
            values.requireArityOrMalformed(fieldName, 1)
            deserFunc(values.single())
        }

    /**
     * Same as [processOptionalField] but throws [MalformedDomainDataException] if the field does not exist
     * in [fieldMap].]
     */
    fun <T> processRequiredField(fieldName: String, deserFunc: (AnyElement) -> T): T =
        processOptionalField(fieldName, deserFunc)
            ?: errMalformed(location, "Required field '${fieldName}' was not found within '$recordTagName' record")

    /**
     * Processes a variadic record field.
     *
     * Throws [MalformedDomainDataException] if [minArity] is > 0 and the field is not present.
     */
    fun <T> processVariadicField(fieldName: String, minArity: Int, deserFunc: (AnyElement) -> T): List<T> {
        val foundFieldValues = fieldMap.remove(fieldName) ?: emptyList()
        foundFieldValues.requireArityOrMalformed(fieldName, minArity..Int.MAX_VALUE)
        return foundFieldValues.map {
            deserFunc(it)
        }
    }

    private fun List<AnyElement>.requireArityOrMalformed(fieldName: String, size: Int) =
        this@requireArityOrMalformed.requireArityOrMalformed(fieldName, IntRange(size, size))

    private fun List<AnyElement>.requireArityOrMalformed(fieldName: String, arityRange: IntRange) {
        if(this.size !in arityRange) {
            errMalformed(
                location,
                "$arityRange values(s) were required to for field '$fieldName' of record '$recordTagName' " +
                    "but ${this.size} was/were supplied.")
        }
    }

    /**
     * After all required an optional fields in a record have been processed by the transformer, this
     * function should be invoked to throw a [MalformedDomainDataException] if any unprocessed fields remain in
     * [fieldMap].  This would indicate that a mis-named field was present.
     */
    fun malformedIfAnyUnprocessedFieldsRemain() {
        if(fieldMap.isNotEmpty()) {
            errUnexpectedField(location, fieldMap.keys.first())
        }
    }
}

/**
 * Does part of the work of deserializing records.
 *
 * Converts the receiver [AnyElement], which must be an expression in the form of:
 *
 * ```
 * '(' <recordTagName> [ '(' <fieldName> <fieldValue> ')' ]... ')'
 * ```
 *
 * To an instance of [IntermediateRecord].
 */
fun SexpElement.transformToIntermediateRecord(): IntermediateRecord {
    val recordTagName = this.head.symbolValue
    val recordFields = this.tail

    val fieldMap = recordFields.map { field: AnyElement ->
        val fieldSexp = field.asSexp()
        fieldSexp.head.symbolValue to fieldSexp.values.tail
    }.toMap()

    return IntermediateRecord(recordTagName, this.metas.location, fieldMap)
}

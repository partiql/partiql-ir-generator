package org.partiql.pig.generator.kotlin

import java.time.OffsetDateTime

/**
 * The properties of [KotlinCrossDomainFreeMarkerGlobals] become global variables for our freemarker template that
 * generates the cross-domain visitor transforms.
 */
@Suppress("unused")
class KotlinCrossDomainFreeMarkerGlobals(
    val namespace: String,
    val transform: KTransform,
    val generatedDate: OffsetDateTime
)
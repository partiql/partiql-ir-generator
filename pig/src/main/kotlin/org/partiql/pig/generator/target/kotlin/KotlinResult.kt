package org.partiql.pig.generator.target.kotlin

import org.partiql.pig.generator.target.kotlin.spec.KotlinFileSpec

class KotlinResult(private val specs: List<KotlinFileSpec>) {

    fun write(action: (KotlinFileSpec) -> Unit) = specs.forEach(action)
}

package org.partiql.pig.generator.ion

import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.domain.toIonElement
import java.io.PrintWriter

/**
 * Generate an Ion representation of each type domain and write it to [output]
 */
fun generateIon(domains: List<TypeDomain>, output: PrintWriter) =
    domains.map(TypeDomain::toIonElement).forEach(output::println)

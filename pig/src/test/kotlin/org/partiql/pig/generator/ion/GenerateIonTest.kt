package org.partiql.pig.generator.ion

import com.amazon.ion.system.IonReaderBuilder
import org.junit.jupiter.api.Test
import org.partiql.pig.domain.parser.parseTypeUniverse
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class GenerateIonTest {
    @Test
    fun `should generate canonical Ion domains`() {
        // Typical type universe (contains domain definitions, permutations, etc.)
        val typeUniverseWithExtensions = """        
        (define test_domain
            (domain 
                (product pair first::ion second::ion)
                (product other_pair first::symbol second::symbol)
                (sum thing
                    (a x::pair)
                    (b y::symbol)
                    (c z::int))))

        (define permuted_domain 
            (permute_domain test_domain
                (exclude pair)
                (include
                    (product pair n::int t::int)
                    (product new_pair m::symbol n::int))
                (with thing
                    (exclude a)
                    (include
                        (d a::pair)
                        (e b::symbol)))))
        """

        // The expected type universe should only contain pure domain definitions (i.e. no permutations)
        val expectedDomains = """
        (define test_domain
            (domain 
                (product pair first::ion second::ion)
                (product other_pair first::symbol second::symbol)
                (sum thing
                    (a x::pair)
                    (b y::symbol)
                    (c z::int))))

        (define permuted_domain 
            (domain
                (product other_pair first::symbol second::symbol)
                (sum thing
                    (b y::symbol)
                    (c z::int)
                    (d a::pair)
                    (e b::symbol))
                (product pair n::int t::int)
                (product new_pair m::symbol n::int)))
        """

        val td = IonReaderBuilder.standard().build(typeUniverseWithExtensions).use { parseTypeUniverse(it) }

        val concretes = td.computeTypeDomains()

        val writer = StringWriter()
        generateIon(concretes, PrintWriter(writer))
        val output = writer.toString()

        val actual = IonReaderBuilder.standard().build(output).use { parseTypeUniverse(it) }

        val expected = IonReaderBuilder.standard().build(expectedDomains).use { parseTypeUniverse(it) }

        assertEquals(expected, actual, "Computed domains should match")
    }
}

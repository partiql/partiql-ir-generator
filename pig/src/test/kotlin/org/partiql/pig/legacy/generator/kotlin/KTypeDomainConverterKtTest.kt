package org.partiql.pig.legacy.generator.kotlin

import com.amazon.ion.system.IonReaderBuilder
import org.junit.jupiter.api.Test
import org.partiql.pig.legacy.parser.parseTypeUniverse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KTypeDomainConverterKtTest {

    private val typeUniverseText = """        
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
        
        (transform test_domain permuted_domain)
        """

    private val typeUniverse = IonReaderBuilder.standard().build(typeUniverseText).use { parseTypeUniverse(it) }
    private val typeDomains = typeUniverse.computeTypeDomains()

    @Test
    fun `it should convert all domains to a KTypeUniverse with transforms`() {
        val ktu = typeUniverse.convertToKTypeUniverse(typeDomains, typeDomains, null)

        val kTypeDomainNames = ktu.domains.map { it.tag }
        assertTrue("test_domain should be present in the kotlin type domains") { "test_domain" in kTypeDomainNames }
        assertTrue("permuted_domain should be present in the kotlin type domains") { "permuted_domain" in kTypeDomainNames }

        assertEquals(1, ktu.transforms.size, "The should be 1 transform between the test_domain and the permuted domain")
    }

    @Test
    fun `it should convert all filtered domains to a KTypeUniverse`() {
        val ktu = typeUniverse.convertToKTypeUniverse(typeDomains, typeDomains.filter { it.tag == "test_domain" }, setOf("test_domain"))

        val kTypeDomainNames = ktu.domains.map { it.tag }
        assertTrue("test_domain should be present in the kotlin type domains") { "test_domain" in kTypeDomainNames }

        assertEquals(0, ktu.transforms.size, "There are no transforms associated with the test_domain")
    }

    @Test
    fun `it should convert all filtered domains to a KTypeUniverse with transforms`() {
        val ktu = typeUniverse.convertToKTypeUniverse(typeDomains, typeDomains.filter { it.tag == "permuted_domain" }, setOf("permuted_domain"))

        val kTypeDomainNames = ktu.domains.map { it.tag }
        assertTrue("permuted_domain should be present in the kotlin type domains") { "permuted_domain" in kTypeDomainNames }

        assertEquals(1, ktu.transforms.size, "There should be a transform associated with the permuted_domain")
    }
}

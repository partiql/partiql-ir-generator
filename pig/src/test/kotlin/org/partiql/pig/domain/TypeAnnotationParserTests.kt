package org.partiql.pig.domain

import com.amazon.ion.system.IonReaderBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.pig.domain.model.DataType
import org.partiql.pig.domain.model.TypeAnnotation
import org.partiql.pig.domain.parser.parseTypeUniverse

class TypeAnnotationParserTests {

    @ParameterizedTest
    @MethodSource("typeAnnotationCases")
    fun typeAnnotationTests(tc: TestCase) {
        val universe = "(define foo (domain ${tc.definition}))"
        val reader = IonReaderBuilder.standard().build(universe)
        val parsed = parseTypeUniverse(reader)
        val domains = parsed.computeTypeDomains()
        assert(domains.size == 1)
        val domain = domains.first()
        val types = domain.userTypes
        assert(types.size == 1)
        val type = types.first()
        tc.assertion(type)
    }

    companion object {

        data class TestCase(
            val definition: String,
            val assertion: (type: DataType.UserType) -> Unit,
        )

        @JvmStatic
        fun typeAnnotationCases() = listOf(
            TestCase("experimental::(product window_partition_list)") {
                assert(it.annotations.contains(TypeAnnotation.EXPERIMENTAL))
            },
            TestCase("experimental::(sum ordering_spec (asc) (desc))") {
                assert(it.annotations.contains(TypeAnnotation.EXPERIMENTAL))
            },
            TestCase("deprecated::(product window_partition_list)") {
                assert(it.annotations.contains(TypeAnnotation.DEPRECATED))
            },
            TestCase("deprecated::(sum ordering_spec (asc) (desc))") {
                assert(it.annotations.contains(TypeAnnotation.DEPRECATED))
            },
            TestCase("(sum ordering_spec deprecated::(asc) experimental::(desc) (other))") {
                (it as DataType.UserType.Sum).variants.forEach { v ->
                    when (v.tag) {
                        "asc" -> assert(v.annotations.contains(TypeAnnotation.DEPRECATED))
                        "desc" -> assert(v.annotations.contains(TypeAnnotation.EXPERIMENTAL))
                        "other" -> assert(v.annotations.isEmpty())
                    }
                }
            },
        )
    }
}

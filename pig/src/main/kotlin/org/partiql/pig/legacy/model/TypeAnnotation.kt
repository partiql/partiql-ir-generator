package org.partiql.pig.legacy.model

/**
 * Annotations that are allowed on a UserType definition
 *
 * Example:
 * ```
 *   (sum my_sum_type
 *       (deprecated::first_variant ...)
 *       (second_variant ...)
 *       (experimental::third_variant ...)
 *   )
 * ```
 */
enum class TypeAnnotation {
    DEPRECATED,
    EXPERIMENTAL;

    companion object {

        /**
         * TODO "safe" valueOf until it's decided what to do with
         *  -  Field identifier used in place of type name (in same sum)
         *  -  Variant identifier used in place of type name (in a different sum)
         *
         *  These two TypeDomainSemanticCheckerTests appear to be asserting on missing(?) functionality
         */
        fun of(v: String): TypeAnnotation? = try {
            valueOf(v.toUpperCase())
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
}

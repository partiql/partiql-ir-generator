package org.partiql.pig.domain.model

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
    EXPERIMENTAL
}

package org.partiql.pig.model

/**
 * Reference to some type.
 */
public sealed interface Type {

    public class Primitive(public val scalar: Scalar) : Type

    public class List(public val items: Type) : Type

    public class Map(public val key: Scalar, public val value: Type) : Type

    public class Set(public val items: Type) : Type

    public class Path(public val path: Array<String>) : Type {

        public val name: String = path[path.size - 1]
    }
}

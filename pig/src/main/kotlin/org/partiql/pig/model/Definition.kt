package org.partiql.pig.model

/**
 * Definition of some type
 */
public sealed interface Definition {

    public val path: Type.Path

    public class Product(
        public override val path: Type.Path,
        public val operands: List<Operand>,
        public val children: List<Definition>,
    ) : Definition {

        override fun toString(): String = "(record ${path.name} todo)"
    }

    public class Sum(
        public override val path: Type.Path,
        public val variants: List<Definition>,
        public val children: List<Definition>,
    ) : Definition {

        override fun toString(): String = "(union ${path.name} todo)"
    }

    public class Enum(
        public override val path: Type.Path,
        public val values: List<String>,
    ) : Definition {

        override fun toString(): String = "(enum ${path.name} todo)"
    }

    public class Fixed(
        public override val path: Type.Path,
        public val size: Int,
    ) : Definition {

        override fun toString(): String = "(fixed ${path.name} $size)"
    }

    public class Unit(public override val path: Type.Path) : Definition {

        override fun toString(): String = "(unit ${path.name})"
    }
}

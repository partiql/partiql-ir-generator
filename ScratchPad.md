# TOOD

## metas

Metas are arbitrary key/value pairs that can be associated with any node. Metas can be used to store metadata about a
node, such as the location in the source text of the grammar element it represents, or the data type of the value
returned by an expression.

## product validation restrictions

## Records vs products

when to chose a record or a product

# PartiQL IR Generator

PIG a compiler framework, domain modeling tool and code generator for tree data structures such as ASTs (Abstract Syntax
Tree), database logical plans, database physical plans, and other intermediate representations. Using PIG, the developer
concisely defines the structure of a tree by specifying named constraints for every possible node and their attribues.
Every constraint is known as a "data type", a collection of data types is known as a "type domain" and a collection of
type domains is known as a "type universe".

## Permuted Domains

Query engines and other kinds of compilers often have numerous such type domains, starting with an AST. Query engines
for example typically parse a query, producing an AST, and then apply multiple passes over the AST to transform it to a
logical plan, then a physical plan, and possibly other intermediate representations. Compiler passes of this sort are
large, complex, and difficult to maintain. PIG's "permuted domains" feature increases the maintainability of such
compiler passes by allowing new type domains to be created by specifying only the *differences* to another type domain.
This avoids having to duplicate the data type definitions that are common to both type domains, allowing more numerous,
smaller, less complex and more maintainable compiler passes.

PIG's permuted domain feature has been heavily inspired by the [Nanopass Framework](https://nanopass.org/).

## Code Generation

PIG generates the following components in Kotlin (and may generate similar components in languages such as Rust in the
future):

- Immutable, strongly typed classes representing each data type within each type domain.
- Abstract base classes for implementing compiler passes:
    - Transform from one type domain to another (the developer must only account for the *differences* between the
      domains!).
    - Transform to modified tree of the same domain.
- Functions to convert each tree to and from [Ion s-expressions](https://amzn.github.io/ion-docs/docs/spec.html#sexp), a
  compact binary format suitable for transmission across process boundaries or to persistent storage.
- Builder functions, for composing trees in code.

---------

## PIG's Type Domain Modeling Language

A simple DSL based on the [Ion text format](https://amzn.github.io/ion-docs/docs/spec.html) is used to define a type
universe. Within a type universe, the developer defines one or more type domains, each of which consists of a number
of `product`, `record` and `sum` type definitions. Together, these type definitions describe the complete structure of a
tree and comprise all the data used to generate the code listed in the previous section.

> **A Note About Terminology**
>
> Although PIG currently only generates code for Kotlin, In the future PIG will generate source code for other
> languages. Thus, we felt we should avoid terms like "enum", "class", "struct", since these all have different
> meanings unique to each language.

Each type definition consists of a name and zero or more definitions for its elements. The element definition consists
of a name and data type.

### `product` Types

Here is a simple example of a tree definition that uses only a single `product` type.

```text
(define sample_type_domain
    (domain
        (product person 
            first::symbol 
            last::symbol 
            children::(* person 0))
    )
)
```

This type domain defines a single `product` type named `person` with three elements: `first`, `last` and a list of at
least 0 `children`.

Element definitions take the following form:

```text
    <property_name>::<data_type>
```

Where:

- `property_name` is the name of the property in the corresponding Kotlin class. For `product` types, this is required.
- `data_type` is the name of the data type of the element. This can be one of the supported primitive types:
  `int`, `symbol`, `bool`, `ion`, or any other data type defined in the same domain (excluding sum variants which will
  be discussed below).

#### Generated Data Model

For the `person` example, PIG generates code that looks like the example below. This class is
a [persistent data structure](https://en.wikipedia.org/wiki/Persistent_data_structure)
and therefore cannot be directly modified. Functions provided to manipulate instances of this class (`withMeta`
and `copy`) always preserve the original version and return modified copies.

```Kotlin
/** The Kotlin implementation of the type domain named `sample_type_domain`. */
class SampleTypeDomain private constructor() {
    // ... other stuff

    /**
     * The Kotlin implementation of the `person` `product` type.
     */
    class Person(
        val first: SymbolPrimitive,
        val last: SymbolPrimitive,
        val children: List<Person>,
        override val metas: MetaContainer
    ) : SampleTypeDomainNode() {

        /** Converts person to its Ion s-expression representation. */
        override fun toIonElement(): SexpElement {
            /* ... */
        }

        /** Creates a copy of [Person] node that is identical to the original but includes the specified key and meta. */
        override fun withMeta(metaKey: String, metaValue: Any): Person {
            /* ... */
        }

        /** Creates a copy of [Person], replacing the existing metas collection. */
        override fun copy(metas: MetaContainer): Person {
            /* ... */
        }

        /**
         * Creates a copy of [Person] with new values for the specified properties.
         *
         * Each parameter corresponds to a property on this class.  If left unspecified, the copy will have
         * the same value as the original.
         */
        fun copy(
            first: SymbolPrimitive = this.first,
            last: SymbolPrimitive = this.last,
            children: List<Person> = this.children,
            metas: MetaContainer = this.metas
        ) {
            /* ... */
        }

        /**
         * Determines if this [Person] is equivalent [other].
         *
         * To be equivalent, [other] must be an instance of [Person] and all of its properties (except [metas]) must be
         * equivalent.
         *
         * This is recursive and applies deeply to all child nodes.
         *
         */
        override fun equals(other: Any?): Boolean {
            /* ... */
        }

        /**
         * Computes a hash code for the current instance of [Person].
         *
         * The has code is computed once, the first time this function is invoked and the value is then re-used.
         *
         * The [metas] property is not an input into the hash code computation.
         */
        override fun hashCode(): Int {
            /* ... */
        }
    }

    // ... other stuff
}
```

The first thing the reader will note is that `Person` resides within the `SampleTypeDomain` class which is being used as
a namespace. Some projects have many type domains sharing the same class names and this helps avoid ambiguity and
namespace pollution.

The next thing the reader might notice is that `Person` implements some functionality provided by Kotlin
[data classes](https://kotlinlang.org/docs/data-classes.html). Such as `.copy`, `.equals` and `.hashCode`. This is
necessary because data classes always include all of their properties in the `.hashCode()` and `.equals()`
implementations, we do include [metas] in the definition of equivalence for any PIG-generated class. (More on metas
later.)

#### Generated Builders

Although the constructor of `Person` seen in the previous section was public, this is not the preferred way of creating
instances. The preferred way involves using `SampleTypeDomain.Builder`. The following code creates a node representing a
person and two children:

```Kotlin
val person = SampleTypeDomain.build {
    person(
        "Billy", "Smith",
        listOf(
            person("Jack", "Smith", listOf()),
            person("Sue", "Smith", listOf())
        )
    )
}
```

This approach has the benefit of being very clear and concise because it is not necessary to fully qualify each type or
to add `import` statements for each the generated each types. This especially true for large projects with many
PIG-generated types and type domains.

The relevant parts of `SampleTypeDomain` which makes this work:

```Kotlin
class SampleTypeDomain private constructor() {

    companion object {
        fun <T : SampleTypeDomainNode> build(block: Builder.() -> T) {
            // ...
        }

        // ... other stuff
    }

    interface Builder {
        fun person(
            first: String,
            last: String,
            children: List<Person> = emptyList(),
            metas: MetaContainer = emptyMetaContainer()
        ): SampleTypeDomain.Person =
            SampleTypeDomain.Person(
                first = first.asPrimitive(),
                last = last.asPrimitive(),
                children = children,
                metas = newMetaContainer() + metas
            )

        // overloads of [person] omitted for brevity.
    }

    // ... other stuff
}
```

### `record` Types

Here is a simple example of a tree definition that uses only a single `record` type.

```text
(define sample_type_domain
    (domain
        (record person 
            first_name::(first symbol)
            last_name::(last symbol) 
            (children (* person 0))
    )
)
```

This `record` stores the same information as the `product` `person` shown above. Aside from the obviously different
syntax, the primary difference between `product` and `record` types are in their s-expressions representations. More
details will be included in this later--for now the reader should know that the names of the elements are included the
s-expressions of a `record` and this is not true for `product` types.

Field definitions take the following form:

```text
    <property_name>::(<field_name> symbol)
```

Where:

- `property_name` is the name of the property in the generated Kotlin class. Unlike `product` types, this is optional.
  When not specified, the `property_name` defaults to the `field_name`.
- `field_name` is the name of the field in the s-expression representation.

The Kotlin class of this record type isn't shown because it has the same API as the `product` type.

Also note that `Builder` interface also includes definitions needed to construct `record` types as well.

### `sum` Types.

`sum` types are used to hold a value that could take on several different, but fixed, types. Only one of the types can
be in use at any one time, and the name of the type, known as a tag, explicitly indicates which one is in use.  `sum`
types are also known as [algebraic](https://en.wikipedia.org/wiki/Algebraic_data_type)  We use the term "variant" to
refer to one of the possible types for a given `sum`.

`sum` types are a natural fit for modeling expressions in a programming language. Let's demonstrate this with an AST for
a very simple toy calculator language that supports integer literals and simple binary expressions:

```text
(define calculator_ast
    (domain
        (sum operator (plus) (minus) (times) (divide) (modulo))
        (sum expr 
            (lit value::int)
            (binary op::operator left::expr right::expr)
        )
    )
)
```

This involves two `sum` types: `operator` and `expr`.

The `operator` `sum` declares five different arithmetic operations: `plus`, `minus`, `times`, `divide`, and `modulo`.
Due to the syntax used here (more on that later), each of these is a `product` type, and each gets a Kotlin class
similar to the example shown above (shown below). However, none of these have any elements.

The `expr` `sum` defines two possible types of expressions that exist our toy calculator:

- `lit`
- `binary`

The Kotlin equivalent of a `sum` type is a [`sealed class`](https://kotlinlang.org/docs/sealed-classes.html).

```Kotlin
class CalculatorAst private constructor() {
    // ... other stuff 
    sealed class Expr(override val metas: MetaContainer = emptyMetaContainer()) : CalculatorAstNode() {
        class Lit(
            val value: LongPrimitive,
            override val metas: MetaContainer = emptyMetaContainer()
        ) : Expr() {
            // ... API same as "person" type from a previous example
        }

        class Binary(
            val op: Operator,
            val left: Expr,
            val right: Expr,
            override val metas: MetaContainer = emptyMetaContainer()
        ) : Expr() {
            // ... API same as "person" type from a previous example
        }
    }
    // ... other stuff
}
```

The builder API shown in previous examples works here as well:

```Kolin
// 1 + 2 * 3
val expr = CalculatorAst.build {
    binary(
        plus(),
        lit(1),
        binary(
            times(),
            lit(2),
            lit(3)))
}   
```

#### Converter\<T\>

PIG generates a facility that allows an instance of a `sum` type to be converted to any other type.

Two examples are provided below.

```Kotlin
val expr = CalculatorAst.build {
    // 1 + 2 * 3 (from the example above)
}

/** Evaluates an instance of CalculatorAst.Expr */
class CalculatorAstEvaluator : CalculatorAst.Expr.Converter<Long> {
    override fun convertLit(node: CalculatorAst.Expr.Lit): Long = node.value.value
    override fun convertBinary(node: CalculatorAst.Expr.Binary): Long =
        when (node.op) {
            is CalculatorAst.Operator.Plus -> convert(node.left) + convert(node.right)
            is CalculatorAst.Operator.Minus -> convert(node.left) - convert(node.right)
            is CalculatorAst.Operator.Times -> convert(node.left) * convert(node.right)
            is CalculatorAst.Operator.Divide -> convert(node.left) / convert(node.right)
            is CalculatorAst.Operator.Modulo -> convert(node.left) % convert(node.right)
        }
}

val evaluator = CalculatorAstEvaluator()
println(evaluator.convert(expr))
// prints: 7


/** Converts an instance of CalculatorAst.Expr into source code. */
class ExprStringifier : CalculatorAst.Expr.Converter<String> {
    override fun convertLit(node: CalculatorAst.Expr.Lit): String = node.value.toString()
    override fun convertBinary(node: CalculatorAst.Expr.Binary): String =
        when (node.op) {
            is CalculatorAst.Operator.Plus -> "${convert(node.left)} + ${convert(node.right)}"
            is CalculatorAst.Operator.Minus -> "${convert(node.left)} - ${convert(node.right)}"
            is CalculatorAst.Operator.Times -> "${convert(node.left)} * ${convert(node.right)}"
            is CalculatorAst.Operator.Divide -> "${convert(node.left)} / ${convert(node.right)}"
            is CalculatorAst.Operator.Modulo -> "${convert(node.left)} % ${convert(node.right)}"
        }
}

val stringifier = ExprStringifier()
println(stringifier.convert(expr))
// prints: 1 + 2 * 3
```

It is common to use [`when`](https://kotlinlang.org/docs/control-flow.html#when-expression) with PIG-generated `sum`
types, which are Kotlin sealead classes. Here's a simple evaluator for our simple calculator language:

#### `record`s as `sum` variants

PIG uses the syntax used to define a variant's elements to determine if it is a `product` or `record`.

```text
(define some_ast
    (domain
        (sum expr
            // This syntax defines a `product` variant.  Note the similarity to a non-variant `product`.
            (binary op::operator left::expr right::expr)
            
            // This syntax defines a `record` variant.  Note the similarity to a non-variant `record`.
            (let 
                (name symbol) 
                (value expr) 
                optional_name::(body expr))
        )
    )
)
```

As with non-variant `record` elements, specifying the property name is optional and defaults to the field name.


### Traversing & Transforming Trees 

PIG generates four different types of tree traversals for performing various tasks such as:

TODO...

#### Visitor

TODO...

#### VisitorFold\<T\>

TODO...

#### VisitorTransform\<T\>










-----------------------

- `product` define a type related to.
- `record` types are similar to product types, however, their s-expression representation includes the names of their
  fields. Thus, order of appearance in the s-expression is not significant. Record types are usually the best choice for
  nodes that have many elements (more than 3) elements.
- `sum` types, which are analagous to [tagged unions](https://en.wikipedia.org/wiki/Tagged_union). Sum types consist of
  a number of `variants`, each of which is a `record` or `product`.

```text
(define simple_domain
    (domain
        (product example_product first::int second::symbol third::bool)
        (record example_record 
            (first int)
            (second symbol)
            (third bool))
            
        (sum example_sum
            (a)
            (b first::int second::symbol third::bool)
            (c 
              (first int)
              (second symbol)
              (third bool))
    )
)
```

```Kotlin
class SampleTypeDomain private constructor() {
    /////////////////////////////////////////////////////////////////////////////
    // Builder
    /////////////////////////////////////////////////////////////////////////////
    companion object {
        @JvmStatic
        fun BUILDER(): Builder = SampleTypeDomainBuilder

        /** Entry point into the Kotlin DSL for instantaiting deeply nested trees. */
        fun <T : SampleTypeDomainNode> build(block: Builder.() -> T) =
            SampleTypeDomainBuilder.block()

        fun transform(element: AnyElement): SampleTypeDomainNode =
            transform(element.asSexp())

        fun transform(element: SexpElement): SampleTypeDomainNode =
            IonElementTransformer().transform(element)
    }

    interface Builder {
        // Tuples 
        /**
         * Creates an instance of [SampleTypeDomain.Person].
         */
        fun person(
            first: String,
            last: String,
            children: kotlin.collections.List<Person> = emptyList(),
            metas: MetaContainer = emptyMetaContainer()
        ): SampleTypeDomain.Person =
            SampleTypeDomain.Person(
                first = first.asPrimitive(),
                last = last.asPrimitive(),
                children = children,
                metas = newMetaContainer() + metas
            )

        /** Default implementation of [Builder] that uses all default method implementations. */
        private object SampleTypeDomainBuilder : Builder

        /** Base class for all SampleTypeDomain types. */
        abstract class SampleTypeDomainNode : DomainNode {
            abstract override fun copy(metas: MetaContainer): SampleTypeDomainNode
            override fun toString() = toIonElement().toString()
            abstract override fun withMeta(metaKey: String, metaValue: Any): SampleTypeDomainNode
            abstract override fun toIonElement(): SexpElement
        }


        /////////////////////////////////////////////////////////////////////////////
        // Tuple Types
        /////////////////////////////////////////////////////////////////////////////
        class Person(
            val first: org.partiql.pig.runtime.SymbolPrimitive,
            val last: org.partiql.pig.runtime.SymbolPrimitive,
            val children: kotlin.collections.List<Person>,
            override val metas: MetaContainer = emptyMetaContainer()
        ) : SampleTypeDomainNode() {

            override fun copy(metas: MetaContainer): Person =
                Person(
                    first = first,
                    last = last,
                    children = children,
                    metas = metas
                )

            override fun withMeta(metaKey: String, metaValue: Any): Person =
                Person(
                    first = first,
                    last = last,
                    children = children,
                    metas = metas + metaContainerOf(metaKey to metaValue)
                )

            override fun toIonElement(): SexpElement {
                val elements = ionSexpOf(
                    ionSymbol("person"),
                    first.toIonElement(),
                    last.toIonElement(),
                    *children.map { it.toIonElement() }.toTypedArray(),
                    metas = metas
                )
                return elements
            }

            fun copy(
                first: org.partiql.pig.runtime.SymbolPrimitive = this.first,
                last: org.partiql.pig.runtime.SymbolPrimitive = this.last,
                children: kotlin.collections.List<Person> = this.children,
                metas: MetaContainer = this.metas
            ) =
                Person(
                    first,
                    last,
                    children,
                    metas
                )

            override fun equals(other: Any?): Boolean {
                if (other == null) return false
                if (this === other) return true
                if (other.javaClass != Person::class.java) return false

                other as Person
                if (first != other.first) return false
                if (last != other.last) return false
                if (children != other.children) return false
                return true
            }

            private val myHashCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
                var hc = first.hashCode()
                hc = 31 * hc + last.hashCode()
                hc = 31 * hc + children.hashCode()
                hc
            }

            override fun hashCode(): Int = myHashCode
        }

        /** Transforms [IonElement] instances into [SampleTypeDomainNode] instances. */
        private class IonElementTransformer : IonElementTransformerBase<SampleTypeDomainNode>() {
            // (details omitted)
        }


        /** */
        open class Visitor : DomainVisitorBase() {
            open fun visitPerson(node: SampleTypeDomain.Person) {}

            open fun walkPerson(node: SampleTypeDomain.Person) {
                visitPerson(node)
                walkSymbolPrimitive(node.first)
                walkSymbolPrimitive(node.last)
                node.children.map { walkPerson(it) }
                walkMetas(node.metas)
            }
        }


        open class VisitorFold<T> : DomainVisitorFoldBase<T>() {
            open protected fun visitPerson(node: SampleTypeDomain.Person, accumulator: T): T = accumulator

            ////////////////////////////////////////////////////////////////////////////
            // Walk Functions
            ////////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////
            // Tuple Types
            //////////////////////////////////////
            open fun walkPerson(node: SampleTypeDomain.Person, accumulator: T): T {
                var current = accumulator
                current = visitPerson(node, current)
                current = walkSymbolPrimitive(node.first, current)
                current = walkSymbolPrimitive(node.last, current)
                node.children.map { current = walkPerson(it, current) }
                current = walkMetas(node.metas, current)
                return current
            }
        }
        abstract class VisitorTransform : DomainVisitorTransformBase() {
            //////////////////////////////////////
            // Tuple Types
            //////////////////////////////////////
            // Tuple Person
            open fun transformPerson(node: SampleTypeDomain.Person): SampleTypeDomain.Person {
                val new_first = transformPerson_first(node)
                val new_last = transformPerson_last(node)
                val new_children = transformPerson_children(node)
                val new_metas = transformPerson_metas(node)
                return if (
                    node.first !== new_first ||
                    node.last !== new_last ||
                    node.children !== new_children ||
                    node.metas !== new_metas
                ) {
                    SampleTypeDomain.Person(
                        first = new_first,
                        last = new_last,
                        children = new_children,
                        metas = new_metas
                    )
                } else {
                    node
                }
            }
            open fun transformPerson_first(node: SampleTypeDomain.Person) =
                transformSymbolPrimitive(node.first)
            open fun transformPerson_last(node: SampleTypeDomain.Person) =
                transformSymbolPrimitive(node.last)
            open fun transformPerson_children(node: SampleTypeDomain.Person) =
                node.children.map { transformPerson(it) }
            open fun transformPerson_metas(node: SampleTypeDomain.Person) =
                transformMetas(node.metas)

        }
    }
```

Let's start with a simple example that covers all of PIG's domain modeling functionality.

### Toy Lang Example

To simplify this discussion, let's define an AST for a fictitious language named "Toy" that supports integer literals, a
simple `let` expression to define variables, functions, and a simple `switch` expression.

```text

// Define the type domain related to  
(define toy_lang
    (domain
    
        // Define a sum type named "expr" that can be used to represent any expression of Toy.
        // Sum types are also known as algebraic data types.
        (sum expr
        
            // Toy has literal integers
            (lit value::int)
            
            (variable name::symbol)
            
            (binary op::operator left::expr right::expr)
            
            (let name::symbol value::expr body::expr)
            
            (function var_name::symbol body::expr)
            
            (switch 
                value::expr 
                branches::(* switch_branch 0)
            )
        )
        
        (sum operator (plus) (minus) (times) (divide) (modulo))
        
        (product switch_branch value::body)
    )
)
```

[comment]: <> (```text)

[comment]: <> (&#40;define )

[comment]: <> (    &#40;domain star_fleet )

[comment]: <> (        &#40;product star_ship name::symbol crew::&#40;* crew_members 1&#41;&#41;)

[comment]: <> (        &#40;record crew_member )

[comment]: <> (            &#40;first_name symbol&#41;)

[comment]: <> (            &#40;middle_names &#40;* symbol 0&#41;&#41;  )

[comment]: <> (            &#40;last_name symbol&#41; )

[comment]: <> (            &#40;age &#40;? int&#41;&#41;)

[comment]: <> (            &#40;rank rank&#41;)

[comment]: <> (        &#41;)

[comment]: <> (        &#40;sum rank)

[comment]: <> (            &#40;cadet)

[comment]: <> (            &#40;chief_petty_officer)

[comment]: <> (            &#40;ensign)

[comment]: <> (            &#40;lieutenant_junior_grade)

[comment]: <> (            &#40;lieutenant)

[comment]: <> (            &#40;lieutenant_commander)

[comment]: <> (            &#40;commander)

[comment]: <> (            &#40;captain)

[comment]: <> (            &#40;admiral)

[comment]: <> (            civilian)


[comment]: <> (    &#41;)

[comment]: <> (```)

There are three kinds of data types, which we will discuss below.

### `product` Types

In PIG parlance, `product` types are tuples with elements that are unnamed. Thus, the position of each element is
important in the s-expression representation. Product types are usually the best choice for nodes that have few (less
than 3) elements.

Example `product` type definition in a simple `starfleet` type domain:

This defines a `person` product type with 3 required elements. S-expression representations of this value looks like:

```
(person Wesley Crusher 14)
(person William Riker 38)
```

In the Kotlin generated code, a class such as the following is generated:

```Kotlin

class Person(
    val first: String,
    val last: String,
    val age: Int
) : StarfleetDomainNode() {
    override fun equals(other: Any?): Boolean = // ...
        override
    fun hashCode(): Int = // ...
}

```

a simple list of constraints for each element of the type.

Here is a type domain defining a simplified PartiQL AST:

```
(define 
    (domain partiql_ast
        // A sum type is effectively a tagged union.  Here, we define a sum 
        // type that can be used to represent any expression in our simplified
        // PartiQL.
        (sum expr
        
            // Within the body of the sum definition, we define every member
            // of that sum type (known as a "variant") using nested 
            // s-expressions.  Within each s-expression, the first element 
            // (always a symbol) is the tag of the variant.  After the tag, 
            // we include definitions for the variant's elements.  All of the 
            // variants below are product types, except for `select` which
            // is a record type.  More on product and variants later.            
        
            // Literal values--can be any Ion value.
            (lit value::ion)
            
            // A simple named variable.
            (id name::symbol)
            
            // Binary expressions, e.g:
            // `foo = bar`, `1 + 2 * 3`, `some_string || 'another string'`, etc.
            (binary op::operator left::expr right::expr)
            
            // Dotted and subscripted expressions, e.g. `foo.bar.bat` or 
            // `foo[42]`.
            (path root::expr subscripts::(* expr 1))
            
            // function calls, e.g. `fibonacci(42)`
            (call func_name::symbol arguments::(* expr 0))
            
            // Select statement
            (select
                project::projection
                from::relation
                (? where::expr)
                (? offset::expr)
                (? limit::expr)
            )
        )
                 
        // All the different binary operators.
        (sum operator (eq) (plus) (minus) (times) (divide) (mod) (concat))
        
        // The prorjection part of a select expression.
        (sum projection
        
            // SELECT *
            (project_star)
            
            // SELECT VALUE <expr>
            (project_value value::expr)
            
            // SELECT <expr> AS <alias> [, <expr> AS <alias> ]...
            (project_list (* project_pair 1))
        )
        
        (sum relation
            // FROM <expr> [AS <as_alias>] (values must be an `expr` that returns a colledction).
            (collection values::expr (? as_alias::symbol))
            
            // <left> [<join_type>] JOIN <right> ON <predicate>
            (join type::join_type left::relation right::join_type predicate::expr)
        )
        
        (sum join_type (inner) (left) (right) (full))

            
        (product project_pair
            column::expr
            (? alias::symbol)
        )
    )
)
```

Using only these three kinds of data types, any tree-like data structure can be modeled.

Each of these types has a corresponding equivalent

| PIG Type  | Kotlin Equivalent                                                     |
|-----------|-----------------------------------------------------------------------|
| `sum`     | [`sealed class`](https://kotlinlang.org/docs/sealed-classes.html)     |
| `product` | [`class`](https://kotlinlang.org/docs/classes.html) (regular classes) |
| `record`  | [`class`](https://kotlinlang.org/docs/classes.html) (regular classes) |

//In the case of Kotlin, the code generated for `product` and `record` types is identical with the exception of the (de)
serialization...

## Examples

```

(define unresolved_logical_algebra
    (permute_domain partiql_ast
        (with expr (exclude select))
            
        (with relation
            (include
                (project project::projection source::relation)
                (filter predicate::expr source::relation)
                (limit count::expr source::relation)
                (offset count::expr source::relation)
            )
        )
    )
)
```

```
SELECT 
    c.name AS CustomerName,
    i.date AS InvoiceDate,
    i.total AS InvoiceTotal
FROM Customer AS c
    INNER JOIN Invoice AS i ON c.CustomerId ON i.CustomerId
WHERE
    c.City = 'Seattle'
OFFSET 100
LIMIT 25
```

AST Representation:

```
(select

    // projection    
    (project_list
        (project_pair (path (id c) (lit name)) CustomerName)
        (project_pair (path (id i) (lit date)) InvoiceDate)
        (project_pair (path (id i) (lit total)) InvoiceTotal))
        
    // from    
    (join
        (inner)
        // left 
        (collection (id Customer) c)
        // right
        (collection (id Invoice) i)
        // predicate
        (binary (eq) (path (id c) (lit CustomerId)) (path (id i) (lit CustomerId))))
                
    // where
    (binary (eq) (path (id c) (lit City)) (lit "Seattle"))
    
    // offset
    (lit 100)
    
    // limit    
    (lit 25)
)
```

- Not easily manipulated due to the way its factored--select node does many things, is very complex and monolitic.
- To ameliorate these issues, we convert to the logical algebra.

Unresolved Logical Algebra:

```
(project
    (project_list
        (project_pair (path (id c) (lit name)) CustomerName)
        (project_pair (path (id i) (lit date)) InvoiceDate)
        (project_pair (path (id i) (lit total)) InvoiceTotal))
    (limit
        (lit 25)
        (offset
            (lit 100)
            (filter
            
                // predicate (originally the where clause)
                (binary (eq) (path (id c) (lit City)) (lit "Seattle"))
                
                // source (originally the from clause)
                (join (inner)
                    // left
                    (collection (id Customer) c)
                    // right
                    (collection (id Invoice) i)
                    // predicate
                    (binary 
                        (eq)
                            (path (id c) (lit CustomerId))
                            (path (id i) (lit CustomerId))))
            )
        )
    )
)
```

Above, but with predicate push-down optimization:

```
(project
    (project_list
        (project_pair (path (id c) (lit name)) CustomerName)
        (project_pair (path (id i) (lit date)) InvoiceDate)
        (project_pair (path (id i) (lit total)) InvoiceTotal))
    (limit
        (lit 25)
        (offset
            (lit 100)
            // source
            (join
                (inner)
                (filter
                    // predicate 
                    (binary
                        (eq)
                        (path (id c) (lit City))
                        (lit "Seattle"))
                    // source 
                    (collection (id Customer) c)
                )
                (collection (id Invoice) i)
                (binary 
                    (eq)
                        (path (id c) (lit CustomerId))
                        (path (id i) (lit CustomerId)))
            )
        )
    )
)
```

## Tools Similar to PIG

These two tools closely related to the role that PIG plays in compiler construction.

- [treecc](https://www.gnu.org/software/dotgnu/treecc/treecc.html)
- [TreeDL](http://treedl.sourceforge.net/treedl/treedl_en.html)

--------------------------------------------------------------------------------------------------------------



PIG reduces the boilerplate required to create compilers, making them easier to understand and maintain.

The PartiQL IR Generator is a compilers framework that focuses on creating creating multiple intermediate
representations which are required by many parts of a compiler that come *after* parsing. PIG can be used to define
object models (known as *type domains*) for all (or most) of the various tree-like data structures that are required to
perform various functions within a compiler. Examples of tree-like data structures include: ASTs (Abstract Syntax Trees)
, database query engine logical & physical plans, and any number of other tree-like intermediate representations that
are employed by a compiler. Each of these type domains has a serialized form consisting
of [Ion s-expressions](https://amzn.github.io/ion-docs/docs/spec.html#sexp) for transmission accross process boundaries
or to persistent storage.  ([Ion](https://amzn.github.io/ion-docs/docs/spec.html) is superset of JSON with a richer type
system.)

PIG also provides facilities that allow the creation of simple compiler passes by changes to the structure of an object
model and

from one pass to the next.  (Inspired by [nanopass](http://nanopass.org/) inspired type domain code generator currently
targeting Kotlin.

PIG exists as a command-line tool that is invoked at build time by a host project such
as [`partiql-lang-kotlin`](https://github.com/partiql/partiql-lang-kotlin). After checking the object model definition
for correctness, PIG generates a Kotlin source file containing:

- Immutable, strongly typed classses to represent each type in an object model in memory.
- Facilities for:
    - Builders fasily instantiating deeply nested instances of the generated classes.
    - Converting each object model to and from its s-expression representation.
    - Manipulating (rewriting) each object model.
    - Converting from one object model to another.

PIG is not a parser generator.



Generator: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-ir-generator/badge.svg?)](https://search.maven.org/artifact/org.partiql/partiql-ir-generator)
Runtime: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-ir-generator-runtime/badge.svg?)](https://search.maven.org/artifact/org.partiql/partiql-ir-generator-runtime)

# The PartiQL I.R. Generator

## PIG Overview

PIG is a compiler framework, domain modeling tool and code generator for tree data structures such as ASTs (Abstract
Syntax Tree), database logical plans, database physical plans, and other intermediate representations. Using PIG, the
developer concisely defines the structure of a tree by specifying named constraints for every node and their attributes.
Every constraint is known as a "data type", a collection of data types is known as a "type domain" and a collection of
type domains is known as a "type universe".

Every type domain has two representations:

- An [Ion s-expression](https://amzn.github.io/ion-docs/docs/spec.html#sexp) representation, allowing type domains to
  serve as a language and platform neutral wire protocol and compact serialization format.
- A strongly typed set of data types specific to a target language such as Kotlin (the only currently supported language
  target).

PIG also provides facilities that allow for manipulation and rewriting of trees for the purposes such as program
optimization, query planning and code generation.

### Permuted Domains

Query engines and other kinds of compilers require numerous tree-like representations of a program, starting with an
AST. Query engines for example typically parse a query, producing an AST, and then apply multiple passes over the AST to
*inrecmentally* transform it to a logical plan, then a physical plan, and possibly other intermediate representations.
Compiler passes and related data structures of this sort are large, complex, and difficult to maintain. PIG's "permuted
domains" feature increases the maintainability of such compiler passes and related data structures by allowing new type
domains to be created by specifying only the *differences* to another type domain. This avoids having to duplicate the
data type definitions that are common to both type domains, allowing more numerous, smaller, less complex and more
maintainable compiler passes.

PIG's permuted domain feature has been heavily inspired by the [Nanopass Framework](https://nanopass.org/).

### Code Generation

PIG generates the following components in Kotlin (and may generate similar components in languages such as Rust in the
future):

- Immutable, strongly typed classes representing each data type within each type domain.
- Abstract base classes for implementing compiler passes that:
    - Transform from one type domain to another (the developer must only account for the *differences* between the
      domains!).
    - Transform to a modified tree of the same domain.
- Functions to convert each tree between instances of the generated classes and its s-expression representation.
- Builder functions, for easily composing deeply nested instances of the strongly typed classes.

## API Status

PIG is mature but its API and the API of the generated Kotlin code is under development and might change.

## PIG's Type Domain Modeling Language

A simple DSL (domain specific language) based on the [Ion text format](https://amzn.github.io/ion-docs/docs/spec.html)
is used to define a type universe. Within a type universe, the developer defines one or more type domains, each of which
consists of a number of `product`, `record` and `sum` type definitions. Together, these type definitions describe the
complete structure of a tree and comprise all the data used to generate the code listed in the previous section.

> **A Note About Terminology**
>
> Although PIG currently only generates code for Kotlin, In the future PIG will generate source code for other
> languages. Thus, we felt we should avoid terms like "enum", "class", "struct", since these all have different
> meanings unique to each language.  

Each type definition consists of a name and zero or more definitions for its elements. The element definition consists
of a name and data type.

### `product` Types

PIG's `product` types are named after the same from 
[type theory](https://en.wikipedia.org/wiki/Type_theory#Product_Type).  Conceptually, `product`s are tuples, 
represented as a `class` in Kotlin.

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

#### Generated Product Type Model - Kotlin

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
[data classes](https://kotlinlang.org/docs/data-classes.html). Such as `.copy`, `.equals` and `.hashCode`.
The `data class` feature of Kotlin can't be used in this case because data classes always include all of their
properties in the `.hashCode()` and `.equals()` implementations, and we do not include [metas] in the definition of
equivalence for any PIG-generated class. (More on metas later.)

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

#### Product Element Restrictions

TODO: describe the way it is today, provide a link to: https://github.com/partiql/partiql-ir-generator/issues/98
(possibly this is better moved to after the s-expression representation is described)

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

### `sum` Types

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

The `expr` `sum` defines two possible types of expressions that exist our toy calculator: `lit` and  `binary`.

The Kotlin equivalent of a `sum` type is a [`sealed class`](https://kotlinlang.org/docs/sealed-classes.html).

#### Generated Sum Type - Kotlin

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

```Kotlin
// 1 + 2 * 3
val expr = CalculatorAst.build {
    binary(
        plus(),
        lit(1),
        binary(
            times(),
            lit(2),
            lit(3)
        )
    )
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
    override fun convertBinary(node: CalculatorAst.Expr.Binary): Long {
        val leftValue = convert(node.left)
        val rightValue = cvonvert(node.right)
        when (node.op) {
            is CalculatorAst.Operator.Plus -> leftValue + rightValue
            is CalculatorAst.Operator.Minus -> leftValue - rightValue
            is CalculatorAst.Operator.Times -> leftValue * rightValue
            is CalculatorAst.Operator.Divide -> leftValue / rightValue
            is CalculatorAst.Operator.Modulo -> leftValue % rightValue
        }
    }
}

val evaluator = CalculatorAstEvaluator()
println(evaluator.convert(expr))
// prints: 7

/** Converts an instance of CalculatorAst.Expr into source code. */
class ExprStringifier : CalculatorAst.Expr.Converter<String> {
    override fun convertLit(node: CalculatorAst.Expr.Lit): String = node.value.toString()
    override fun convertBinary(node: CalculatorAst.Expr.Binary): String {
        val leftString = convert(node.left)
        val rightString = cvonvert(node.right)
        return when (node.op) {
            is CalculatorAst.Operator.Plus -> "$leftString + $rightString"
            is CalculatorAst.Operator.Minus -> "$leftString - $rightString"
            is CalculatorAst.Operator.Times -> "$leftString * $rightString"
            is CalculatorAst.Operator.Divide -> "$leftString / $rightString"
            is CalculatorAst.Operator.Modulo -> "$leftString % $rightString"
        }
    }
}

val stringifier = ExprStringifier()
println(stringifier.convert(expr))
// prints: 1 + 2 * 3
```

#### `record`s as `sum` variants

PIG uses the syntax used to define a variant's elements to determine if it is a `product` or `record`.

```text
(define toy_ast
    (domain
        (sum expr
            // This syntax defines a `record` variant.  Note the similarity to a non-variant `record`.
            (let
                (name symbol)
                (value expr)
                optional_name::(body expr))
                
            // This syntax defines a `product` variant.  Note the similarity to a non-variant `product`.
            (binary op::operator left::expr right::expr)
        )
        // ... 
    )
)
```

As with non-variant `record` elements, specifying the property name is optional and defaults to the field name.

### Metas

Metas are arbitrary key/value pairs that can be associated with any node. Metas can be used to store metadata about a
node, such as the location in the source text of the grammar element it represents, or the data type of the value
returned by an expression.

## Traversing & Transforming Trees

PIG generates four different types of tree traversals for performing various tasks. Each is described in the
sub-headings of this section.

For the examples below we will use an `enhanced_calculator_ast`. This is like the `calculator_ast` from before but also
includes a `let` and `variable` expressions.

```text
(define enhanced_calculator_ast
        (domain
            (sum expr
                 (lit value::int)
                 (binary op::operator left::expr right::expr)
                 (let name::symbol value::expr body::expr)
                 (variable name::symbol) 
             )

            (sum operator (plus) (minus) (times) (divide) (modulo))
        )
)
```

### `VisitorTransform`

The PIG-generated `VisitorTransform` base classes are by far the most commonly used type of tree traversal. Using a
`VisitorTransform`, it is possible to implement many kinds of compiler optimizations, static type inference, expression
normalization, and syntactic de-sugaring.

By default, every type domain gets one `VisitorTransform` class which can be used to effectively perform a deep copy of
the input tree. Functions of the `VisitorTransform` may be overridden by derived classes to customize how these copies
of are made, allowing for trees to be manipulated in a variety of ways.  In reality, the `VisitorTransform` does not
actually copy all nodes of the tree--only the modified nodes and their parent nodes are replaced.   (This is possible
because PIG-generated nodes are always immutable.)`

The example below shows how to perform constant folding with our `enhanced_calculator_ast`:

```Kotlin
// TODO!!  For now, please search for examples in the pig-tests sub-project.
```

### Permuted Domains and Cross-Domain `VisitorTransform`s

TODO...  For now, please search for examples in the pig-tests sub-project.

### `Visitor`

Plain visitors are most useful for performing simple semantic checks or validation of type domain instances, but are 
not usually very useful for performing most *transformations* of trees into some other types of data.

Every PIG-generated `Visitor` class receives one `visit*` and `walk*` function.

This is different from the typical visitor pattern in that the `walk*` functions reside on the `Visitor` class and
*not* on the classes for each node. This allows for the `walk*` functions to be customized as needed, for example, if
the traversal order needs to be modified, of custom logic needs to be applied between child nodes.

An example visitor for the `enhanced_calculator_ast` domain resides below.

```Kotlin
open class Visitor : DomainVisitorBase() {
    // Sum Type: Expr
    protected open fun visitExpr(node: EnhancedCalculatorAst.Expr) {}
    protected open fun visitExprLet(node: EnhancedCalculatorAst.Expr.Let) {}
    protected open fun visitExprVariable(node: EnhancedCalculatorAst.Expr.Variable) {}
    protected open fun visitExprLit(node: EnhancedCalculatorAst.Expr.Lit) {}
    protected open fun visitExprBinary(node: EnhancedCalculatorAst.Expr.Binary) {}

    // Walk Functions

    // Sum Type: Expr
    open fun walkExpr(node: EnhancedCalculatorAst.Expr) {
        // Calls visitExpr
        visitExpr(node)

        // Dispatch to walkExpr* function for specific variant of the `expr` sum type.
        when (node) {
            is EnhancedCalculatorAst.Expr.Let -> walkExprLet(node)
            is EnhancedCalculatorAst.Expr.Variable -> walkExprVariable(node)
            is EnhancedCalculatorAst.Expr.Lit -> walkExprLit(node)
            is EnhancedCalculatorAst.Expr.Binary -> walkExprBinary(node)
        }
    }

    open fun walkExprLet(node: EnhancedCalculatorAst.Expr.Let) {
        // calls `visitExprLet`
        visitExprLet(node)

        // calls `walk*` for each child node
        walkSymbolPrimitive(node.name)
        walkExpr(node.value)
        walkExpr(node.body)
        walkMetas(node.metas)

    }

    open fun walkExprVariable(node: EnhancedCalculatorAst.Expr.Variable) {
        /* calls visitExprVariable, then walkExpr for each child node. */
    }

    // And so on for all expr variants...

    // Sum Type: `operator` was removed for brevity.  Like `expr`, the `operator` sum type gets
    // one visitOperator and walkOperator function, and for each of its variants: one visitOperator*
    // and one walkOperator* function variants, just like `expr` does.
}
```

#### `Visitor` Example

The simple example below (using the `enhanced_calculator_ast` type domain from a previous example) shows how to prevent
certain names from being used as variables:

```Kotlin 
class VariableThatShallNotBeNamedValidator : EnhancedCalculatorAst.Visitor() {
    override fun visitExprLet(node: EnhancedCalculatorAst.Expr.Let) {
        if(node.name.text == "voldemort") {
            error("A variable was named after he who shall not be named.")
        }
    }
}

val x = EnhancedCalculatorAst.build {
    // let voldemort = 42 in 43
    let("voldemort", lit(42), lit(43))
}

val validator = VariableThatShallNotBeNamedValidator()
validator.walkExpr(x) // <-- throws IllegalStateException("A variable was named...")
```

### `VisitorFold<T>`

Each type domain gets a class named `VisitorFold<T>`, implementations of which are often used to extract data from trees
either in list or aggregate form. This is similar to a
[functional fold](https://en.wikipedia.org/wiki/Fold_(higher-order_function)) except that it applies to every single
node of a tree instead of to the items in a collection.

A `VisitorFold<T>` implementation is similar in structure to the visitors described in the previous section, but each
`visit*` and `walk*` function has a second argument: the accumulator. The `T` type argument is the type of the
accumulator.

```Kotlin
open class VisitorFold<T> : DomainVisitorFoldBase<T>() {
    // Sum Type: Expr
    open protected fun visitExpr(node: EnhancedCalculatorAst.Expr, accumulator: T): T = accumulator
    open protected fun visitExprLit(node: EnhancedCalculatorAst.Expr.Lit, accumulator: T): T = accumulator
    open protected fun visitExprBinary(node: EnhancedCalculatorAst.Expr.Binary, accumulator: T): T = accumulator
    open protected fun visitExprLet(node: EnhancedCalculatorAst.Expr.Let, accumulator: T): T = accumulator
    open protected fun visitExprVariable(node: EnhancedCalculatorAst.Expr.Variable, accumulator: T): T = accumulator

    // Sum Type: Expr
    open fun walkExpr(node: EnhancedCalculatorAst.Expr, accumulator: T): T {
        // Calls visitExpr 
        val current = visitExpr(node, accumulator)

        // Dispatch to walkExpr* function for specific variant of the `expr` sum type.
        return when (node) {
            is EnhancedCalculatorAst.Expr.Lit -> walkExprLit(node, current)
            is EnhancedCalculatorAst.Expr.Binary -> walkExprBinary(node, current)
            is EnhancedCalculatorAst.Expr.Let -> walkExprLet(node, current)
            is EnhancedCalculatorAst.Expr.Variable -> walkExprVariable(node, current)
        }
    }

    open fun walkExprBinary(node: EnhancedCalculatorAst.Expr.Binary, accumulator: T): T {
        var current = accumulator
        current = visitExprBinary(node, current)
        current = walkOperator(node.op, current)
        current = walkExpr(node.left, current)
        current = walkExpr(node.right, current)
        current = walkMetas(node.metas, current)
        return current
    }

    // And so on for all expr variants...    

    // Generated code for the `operator` sum type was removed for brevity.  Like `expr`, the `operator` sum type gets
    // one `visitOperator` and `walkOperator` function, and each of its variants also gets one `visitOperator*` and
    // one `walkOperator*` function.
}
```

#### `VisitorFold<T>` Example

The example below returns a list of all variables defined in an `enhanced_calculator_ast`:

```Kotlin
class VariableFinder : EnhancedCalculatorAst.VisitorFold<List<String>>() {
    override fun visitExprLet(node: EnhancedCalculatorAst.Expr.Let, accumulator: List<String>): List<String> =
        accumulator + listOf(node.name.text)
}

// This expression declares two variables, 'meaning_of_life' and 'zaphod'.
val x = EnhancedCalculatorAst.build {
    let(
        name = "meaning_of_life",
        value = lit(42),
        body = let(
            name = "zaphod",
            value = lit(43),
            body = binary(plus(), variable("meaning_of_life"), variable("zaphod"))
        )
    )
}

val finder = VariableFinder()
val foundVariables = finder.walkExpr(x, emptyList())

println(foundVariables)
// prints:
// [meaning_of_life, zaphod]
```

## S-Expression Representation of Data Types

TODO...

## Using PIG In Your Project

There are two components of PIG to be aware of:  the
[code generator](https://search.maven.org/artifact/org.partiql/partiql-ir-generator) and the
[runtime library](https://search.maven.org/artifact/org.partiql/partiql-ir-generator-runtime). Both are available
in [Maven Central](https://search.maven.org/search?q=partiql-ir-generator).  **Both of them must be same version.**

### Using Gradle

There are [plans to make a Gradle plugin for PIG](https://github.com/partiql/partiql-ir-generator/issues/102) but one
has not been completed yet.

Without the aforementioned plugin, the best way to use pig with gradle is:

- Add a dependency on PIG in your project's `buildSrc/build.gradle` file. This will make the API of PIG available to all
  other `build.gradle` files in your project.
  ([Example](https://github.com/partiql/partiql-lang-kotlin/blob/main/buildSrc/build.gradle#L9))
- Add a dependency on PIG's runtime library in your project.  
  ([Example](https://github.com/partiql/partiql-lang-kotlin/blob/28701e23cf3bd397a67e8d9ab4f68feff953aea1/lang/build.gradle#L48))
- Add a custom task that uses PIG's internal
  APIs. ([Example](https://github.com/partiql/partiql-lang-kotlin/blob/28701e23cf3bd397a67e8d9ab4f68feff953aea1/lang/build.gradle#L64-L87))
- Make sure your custom task executes *before* the `compileKotlin` task.
  ([Example](https://github.com/partiql/partiql-lang-kotlin/blob/28701e23cf3bd397a67e8d9ab4f68feff953aea1/lang/build.gradle#L89))

### Other Build Systems

If you are not using Gradle, it will be necessary to invoke PIG via the command line.

At build time and before compilation of your application or library, the following should be executed:

```
pig \
    -u <type universe.ion> \
    -t kotlin \ 
    -n <namespace> \ 
    -o path/to/package/<output file>
```

- `<type universe.ion>`:  path to the Ion text file containing the type universe
- `<output file>`: path to the file for the generated code
- `<namespace>`: the name used in the `package` statement at the top of the output file

Execute: `pig --help` for all command-line options.

#### Obtaining the PIG Executable

To obtain the `pig` executable:

- Clone this repository.
- Check out the tag of the [release](https://github.com/partiql/partiql-ir-generator/releases) you wish to utilize,
  e.g. `git checkout v0.4.0`
- Execute `./gradlew assemble`

After the build completes, the `pig` executable and dependencies will be located
in `pig/build/distributions/pig/pig-x.y.z.[tar.gz|zip]`.

**Finally, make sure that the version of the `partiql-ir-generator-runtime` library that you are using corresponds to
the version of the executable.**

Verify this with the `--version` command line option of
PIG.  ([When it becomes available](https://github.com/partiql/partiql-ir-generator/issues/103).)

## Appendices

### Type Universe Grammar

```
// Top level
type_universe ::= <stmt>...
definition ::= '(' 'define' symbol <domain_definition> ')'
stmt ::=  <definition> | <transform>

// Domain
domain_definition ::= <domain> | <permute_domain>
domain ::= '(' 'domain' <type_definition>... ')'
type_definition ::= <product_definition> | <sum_definition> | <record_definition>

// Product
product_definition ::= '(' 'product' <product_body>')'
product_body ::= symbol ( symbol::<type_ref> )...

// Record
record_definition ::= '(' 'record' <record_body> ')'
record_body ::= symbol ('(' symbol ( [symbol::]<field_definition> )... ')')
field_definition ::= '(' symbol <type_ref> ')'

// Sum
sum_definition ::= '(' 'sum' symbol <variant_definition>...')'
variant_definition ::= '(' symbol (<product_body> | <record_body>) ')'

// Domain permutation
permute_domain ::=
    '(' 
        'permute_domain' symbol 
            ( 
                  '(' 'exclude' symbol... ')' 
                | '(' 'include' <type_definition>... ')'  
                | <with> 
            )...
    ')'

with ::=
    '(' 'with' symbol 
        (
              '(' 'exclude' symbol... ')' 
            | '(' 'include' ( '(' <product_body> ')' )... ')' 
        )...
    ')'
    
// Transforms
transform ::= '(' 'transform' symbol symbol ')'

// Type references
type_ref ::= ion_type
           | symbol                     
           | '(' '?' symbol ')'
           | '(' '*' symbol int ')'
    
ion_type ::= 'int' | 'symbol' | 'bool' | 'ion'     
```

#### Type References

##### `ion_type`

When the name of a specific Ion type is used, the equivalent type in the target language is used in the generated code
and the transformation code is generated to expect and produce Ion values of that type. When the name `ion` is used, the
type of the Ion implementation's DOM node (i.e. `IonElement`) is used and the transformation code allows any Ion value.
Today, the only reason the `ion` type is needed is to facilitate the `lit` PartiQL AST node, whose only argument is an
arbitrary Ion value.

For the Kotlin language, the mapping between Ion types and Kotlin types is the same as used by `ion-java`. For the
initial revision we will only support the following subset of Ion types which are required by the PartiQL AST, although
we can expect that needs will arise for other types at a later date.

| PIG Type | Kotlin Type  |
|--------------|--------------|
| `int`        | `Int`        |
| `symbol`     | `String`     |
| `bool`       | `Boolean`    |
| `ion`        | `AnyElement` |

##### Arity

A type reference includes a specification of arity. Arity can be `required`, `optional`, or `variadic`. The name of any
type without adornment indicates that it is `required`.

- `<type_name>` - Indicates that the type is required and appears exactly once.
- `(? <type_name>)` - Indicates that the type is optional. All optional types are represented by `null` when not
  present.
- `(* <type_name> [n])` - Indicates that the type may appear *at least* `n` times. When unspecified, `n` is zero.

##### Arity Ordering

Within the list of a `product`'s elements, the following rules must be obeyed:

- Zero or more `required` types must appear first. After any `required` types none or *one* of the following is allowed:
    - 1 or more `optional` types
    - A single `variadic` argument

In other words, `product`s may have any number of `required` types and cannot have both `optional` and
`variadic` types. If `variadic` type is present, only one is allowed.

These constraints exist to reduce the complexity of the generated code and its uses.

## License

This project is licensed under the Apache-2.0 License.


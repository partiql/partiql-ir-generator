[![Build Status](https://travis-ci.org/partiql/partiql-ir-generator.svg?branch=master)](https://travis-ci.org/partiql/partiql-ir-generator)

# The PartiQL I.R. Generator

PIG is a compiler framework, domain modeling tool and code generator for tree data structures such as ASTs (Abstract Syntax Tree), database logical plans, database physical plans, and other intermediate representations.  Using PIG, the developer concisely defines the structure of a tree by specifying named constraints for every node and their attributes. Every constraint is known as a "data type", a collection of data types is known as a "type domain" and a collection of type domains is known as a "type universe".

Every type domain has two representations:

- An [Ion s-expression](https://amzn.github.io/ion-docs/docs/spec.html#sexp) representation, allowing type domains to serve as a language and platform neutral wire protocol and compact serialization format.
- A strongly typed set of data types specific to a target language such as Kotlin.

PIG also provides facitiles that allow for manipulation and rewriting of trees for the purposes of program optimizaiton, query planning and code generation.

## Permuted Domains

Query engines and other kinds of compilers require numerous tree-like representations of a program, starting with an AST.  Query engines for example typically parse a query, producing an AST, and then apply multiple passes over the AST to transform it to a logical plan, then a physical plan, and possibly other intermediate representations.  Compiler passes of this sort are large, complex, and difficult to maintain.  PIG's "permuted domains" feature increases the maintainability of such compiler passes by allowing new type domains to be created by specifying only the *differences* to another type domain.  This avoids having to duplicate the data type definitions that are common to both type domains, allowing more numerous, smaller, less complex and more maintainable compiler passes.

PIG's permuted domain feature has been heavily inspired by the [Nanopass Framework](https://nanopass.org/).

## Code Generation

PIG generates the following components in Kotlin (and may generate similar components in languages such as Rust in the future): 

- Immutable, strongly typed classes representing each data type within each type domain.
- Abstract base classes for implementing compiler passes that:
    - Transform from one type domain to another (the developer must only account for the *differences* between the domains!).
    - Transform to a modified tree of the same domain.
- Functions to convert each tree between instances of the generated classes and its s-expression representation.
- Builder functions, for easily composing deeply nested instances of the strongly typed classes.

## API Status

PIG is usable now but its API and the API of the generated Kotlin code is under development and might change.

## Definitions

- `type universe`: A collection of type domains.
- `type domain`: A collection of data types, instances of which can be combined together to form a tree.  Also referred 
to simply as "domain".
- `permuted domain`: A type domain that permutes a copy of another domain by removing types, adding types or altering 
sum types, creating a new, independent domain. 
- `data type`: All of Ion's data types plus sum and product types which are composed of other data types.
- `product` : also known as n-tuple.  Similar to an array, but the elements of the product types may have. 
different data types.  The s-expression representations of products do not have names, however names are
required for code generation of target languages that do not have native support for unnamed tuples.  
- `sum type`: a.k.a. [tagged union](https://en.wikipedia.org/wiki/Tagged_union).
- `record`: a product or variant with named elements.
- `variant`: An element of a sum type which consists of a name and zero or more types.
- `element`: A slot within a product or variant which holds an instance of data of a specific data type.

## The Problem Solved By PIG

Simply put, the goal of the PIG project is to automate the creation and maintenance of the numerous tree data
structures required by PartiQL.  PIG generates Kotlin classes to represent data type in a domain and generates:

- One Kotlin class per node type.
- `Object.equals` and `object.hashCode` implementations
- Four different variations of the [visitor pattern](https://en.wikipedia.org/wiki/Visitor_pattern):
    - Plain visitors, which can be used to perform simple semantic checks.
    - Folding visitors, which can be used to extract data from a tree.
    - Rewriting visitors for simple transformations of trees.
    - Conversion visitors, one for each sum type, to convert from a sum type to any other type.
- Components to transform between the generated classes and Ion s-expressions and to check the structure of the 
structure of the s-expression representation.

### Why PIG

These components took a long time to create and come with very non-trivial maintenance overhead.  Changes to either 
the `ExprNode` model or `V0` AST have downstream impacts that are difficult to predict and often require performing 
"shotgun surgery" at an indeterminate number of locations in the (de)serialization code and rewrite rules.

We will soon need additional domain models other than `ExprNode` and the `V0` AST and each new domain model only 
multiplies these problems.

PIG aims to improve developer productivity and reduce risk associated with evolving these domain models by 
automating the creation of all of the above components for all of the domain models required by PartiQL
and the PartiQL rewriter.

## Code generation with PIG

Given a type universe definition, PIG will generate all of the domain objects (classes) and transformation 
code to convert to and from Ion s-expressions for a target language. Currently the only supported target language is 
Kotlin.  PIG also supports creating new type domains by expressing them as permutations of other domains.

For the Kotlin target, PIG can (will) generate:

- Kotlin classes for each type domain
- Code to transform instances of the Kotlin code to an from `IonElement`.
- TO DO: 2 Kotlin DSLs that allow specifying and rewriting trees to be expressed with concise syntax that is checked at
compile-time by the Kotlin compiler:
  - Rewriting components.

Today, PIG is already capable of generating replacements for `ExprNode`, the `V0` AST, and their
transformation code.  

### Example Type Universe

```
// This is an AST (abstract syntax tree) for a simple hypothetical programming language named "Toy".
// Toy has literals, variables, basic arithmetic and functions that accept a single argument. 

(define toy_lang
    (domain
        (sum operator (plus) (minus) (times) (divide) (modulo))
    (sum expr
        (lit value::ion)
        (variable name::symbol)
        (not expr::expr)
        (nary op::operator operands::(* expr 0))
        (let name::symbol value::expr body::expr)
        (function var_name::symbol body::expr))))

// Define another type domain which extends "toy_lang" by removing named variables and adding DeBruijn indices:
(define toy_lang_indexed
    (permute_domain toy_lang
        (with expr
            (exclude variable let)
            (include
            // We are adding the `index` element here.
            (variable name::symbol index::int)
            (let name::symbol index::int value::expr body::expr)))))
```

### Generated Kotlin Domain Model Sample

See the sample generated code [here](pig-tests/src/org/partiql/pig/tests/generated/toy-lang.kt).

### Typical Use Of Generated Domain Models

The basic processing steps of a hypothetical compiler for the Toy language that utilizes PIG follows.  

1. Source code is parsed and a parse tree is produced.
2. The parse tree is converted to an instance of `toy_lang.expr` using a Kotlin DSL (a la `AstBuilder`)
generated by PIG.
3. The instance of `toy_lang.expr` is then converted to an instance of `IonElement` by invoking its `toIonElement()` 
function.
4. The `IonElement` is passed through a rewrite rule which resolves the index of all `variable` and `let` nodes, which
results in another `IonElement` instance. This instance however has integers in place of variable names.
5. The new `IonElement` is then passed to `toy_lang_nameless.transformer`.  If the rewrite rule produced a tree which 
is invalid for the `toy_lang_nameless` domain, an exception is thrown at this time, otherwise, an instance of 
`toy_lang_nameless.expr` is produced.
6. The instance of `toy_lang_nameless.expr` is then used to generate executable code.

Steps 3-5 can be repeated for different rewrite rules, each of which *may* produce an instance of a data type in a 
different type domain.  After each rule is executed, it is checked against the expected domain by invoking its 
transformer.

### Example Uses of the Generated Domain Model

#### Creating Instances of `toy_lang` Types

Note that the `toy_lang.build` is not generated yet but will be soon.

```Kotlin
val onePlusOne = toy_lang.build { plus(lit(ionInt(1)), lit(ionInt(1))) }

val defineFunc = toy_lang.build { 
    let(
        "someFunc", 
        function(
            "someArg", 
            plus(
                variable("someArg"), 
                lit(1))),
        call(
            variable("someFunc"), 
            lit(1)))
}
```

#### Domain Type Instance -> `IonElement` Transformation

Transformation to `IonElement` is accomplished by invoking the `.toIonElement()` node which is present on every 
generated type:

```Kotlin
println(onePlusOne.toIonElement().toIonText())
// prints:  (+ (lit 1) (lit 1))

println(defineFunc.toIonElement().toIonText())
// prints: (let someFunc (function someArg (+ (variable someArg) (lit 1)) (call (variable someFunc) (lit 1)))
```

#### `IonElement` -> Domain Type Instance Transformation

```Kotlin
val sexpTree = createIonElementLoader("(+ (lit 1) (lit 1))").loadSingleElement()
val anotherOnePlusOne = toy_lang.transformer().transform(sexpTree)

// The following assertion will succeed (referencing `onePlusOne` from above):
assertEquals(onePlusOne, anotherOnePlusOne)
```

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

#### PIG Phases

The processing phases of PIG are:  

- Parsing of type universe Ion file into an `IonElement`.
- Transformation to target language neutral domain objects (`TypeUniverse`, et al)
- Check for errors in type domain (undefined or duplicate names, etc)
- Apply all permutations to domains by cloning the domain being permuted while applying the `include` and 
`exclude` entries.
- Conversion to target language specific domain objects (`KotlinTypeUniverse`, et al)
- Code generation in target language using an Apache FreeMarker template.

#### Type References

##### `ion_type`

When the name of a specific Ion type is used, the equivalent type in the target language is used in the generated code 
and the transformation code is generated to expect and produce Ion values of that type. When the name `ion` is used, 
the type of the Ion implementation's DOM node (i.e. `IonElement`) is used and the transformation code allows any Ion 
value. Today, the only reason the `ion` type is needed is to facilitate the `lit` PartiQL AST node, whose only argument 
is an arbitrary Ion value.

For the Kotlin language, the mapping between Ion types and Kotlin types is the same as used by `ion-java`.  For the 
initial revision we will only support the following subset of Ion types which are required by the PartiQL AST, although 
we can expect that needs will arise for other types at a later date.

| PIG Type | Kotlin Type  |
|--------------|--------------|
| `int`        | `Int`        |
| `symbol`     | `String`     |
| `bool`       | `Boolean`    |
| `ion`        | `AnyElement` |

##### Arity

A type reference includes a specification of arity.  Arity can be `required`, `optional`, or `variadic`.  The name 
of any type without adornment indicates that it is `required`.

- `<type_name>` - Indicates that the type is required and appears exactly once. 
- `(? <type_name>)` - Indicates that the type is optional.  All optional types are represented by `null` when not 
present. 
- `(* <type_name> [n])` - Indicates that the type may appear *at least* `n` times.  When unspecified, `n` is zero.

##### Arity Ordering

Within the list of a product's and sum variant's types, the following rules must be obeyed:

- Zero or more `required` types must appear first. After any `required` types none or *one* of the following is 
allowed:
    - 1 or more `optional` types
    - A single `variadic` argument

In other words, `product`s may have any number of `required` types and cannot have both `optional` and 
`variadic` types.  If `variadic` type is present, only one is allowed. 
 
These constraints exist to reduce the complexity of the generated code and its uses.

##### Named Elements

There are two types of records.  Record data types and record sum variants.  Records are similar to products however
their fields are explicitly and uniquely named and they have a different s-expression representation. 

Records are most useful when more than ~4 elements are needed and when some number of them are optional (as 
is the case with the PartiQL AST's `select` node).

The generated `IonElement` transformer for a record allows the named elements of a record to appear in any order, but 
will always render them in the same order as specified in the product or variant definition. Furthermore, instead of 
using Ion `null` values to indicate that an element has not been specified, unspecified elements are simply not 
rendered in the `IonElement` representation. 

The rules described under the "Arity Ordering" section above do not apply to records.  As a result, `required`, 
`optional` and `variadic` elements may appear in any order.  Additionally, any number of `variadic` elements are 
allowed.

The public API of the Kotlin code generated for records is similar to that of their non-named counterparts except that
the names of the elements are used as their identifiers instead of a synthetic name.  Additionally, record constructors 
do not accept `vararg` arguments which means that **TODO: chose one of the following**: 

1. the constructors of record types must validate the minimum arity of variadic elements at runtime
1. the minimum arity of variadic elements is always zero (and PIG fails the build otherwise). 

An example of a domain-level record:

```
(define demo_domain 
    (domain
        ...
        (record mathematician
            (first_name symbol)
            (last_name symbol)
            (age int)
            (games_of_life_conceived int)
        ...)))

// Example s-expression representation:
(programmer
    (first_name John)
    (last_name Conway)
    (age 82)
    (games_of_life_conceived 1))
```

An example of a sum variant record:    

```
(define partiql 
    (domain
        ...
        (sum expr
            ...
            (select
                (project projection)
                (from from_source)
                (where (? expr))
                (group_by (? group_specificaiton))
                (having (? expr))
                (limit (? expr)))
            ...)
        ...)))
```

##### Naming Conventions

All nameable components (type domains, types, elements, etc) of a type universe file should be named using the  
[`snake_case`](https://en.wikipedia.org/wiki/Snake_case) naming convention.

The language target may convert this to another naming convention.  For instance, the Kotlin target converts
`snake_case` to [`camelCase` or `PascalCase`](https://en.wikipedia.org/wiki/Camel_case) as appropriate to 
the context in order to provide an idiomatic API to the generated code.

Future language targets may perform similar conversions.

##### Element Naming

Two types of names exist for the elements of products and records: *identifiers* and *tags*.  Tags are used
by records as a field key in the s-expression representation of a record, while identifiers are used in the 
generated code of both records and products.  For both records and products: an identifier can be specified with 
a single Ion annotation that prefixes the element definition.  For example, given following type definitions:

```
(record person
    first_name::(f symbol)
    (mi (? symbol)) 
    last_name::(l symbol))
```

An example s-expression representation would be:

```
(person (f James) (mi T) (l Kirk))
```

While the generated Kotlin class is:

```Kotlin
class Person(
    val firstName: SymbolPrimitive,
    val mi: SymbolPrimitive?,
    val lastName: SymbolPrimitive
): ...
```

That the second property has the `mi` name which was used because the identifier was not specified for the `mi`
field in the record definition. 

Note that identifiers only need to be specified if the name of an element in the generated code should be different
than the tag s-expression representation.  If unspecified (as is 'mi' element above), element's identifier defaults 
to the tag.

Unlike record elements, product element defintions must include identifiers. 

```      
(product int_pair first::int second::int)
```


#### Visitors

TODO: other types of visitors documented here.

##### Sum Type Converters

The Kotlin target generates a `Converter<T>` interface with methods that allow conversion of each variant type to 
any `T`.

In the example below, the `expr` and `operator` sum types are converted to string representations.

```Kotlin
val toyOperatorConverter = object : ToyLang.Operator.Converter<String> {
    override fun convertPlus(node: ToyLang.Operator.Plus): String = "+"
    override fun convertMinus(node: ToyLang.Operator.Minus): String = "-"
    override fun convertTimes(node: ToyLang.Operator.Times): String = "*"
    override fun convertDivide(node: ToyLang.Operator.Divide): String = "/"
    override fun convertModulo(node: ToyLang.Operator.Modulo): String = "%"
}

val toyExprConverter = object : ToyLang.Expr.Converter<String> {
    override fun convertLit(node: ToyLang.Expr.Lit): String = "${node.value}"
    override fun convertVariable(node: ToyLang.Expr.Variable): String = node.name.text
    override fun convertNot(node: ToyLang.Expr.Not): String = "!${convert(node.expr)}"
    override fun convertNary(node: ToyLang.Expr.Nary): String {
        // This Converter<T> implementation isn't responsible for converting ToyLang.Operator instances.
        // delegate to toyOperatorConverter for that.
        val op = toyOperatorConverter.convert(node.op)
        return node.operands.joinToString(" $op ") { convert(it) }
    }

    override fun convertLet(node: ToyLang.Expr.Let): String =
        "let ${node.name.text} = ${convert(node.value)} in ${convert(node.body)}"

    override fun convertFunction(node: ToyLang.Expr.Function): String =
        "fun (${node.varName.text}) -> ${convert(node.body)}"
}

// Example use:
val ast = ToyLang.build {
  let(
    "x",
    lit(ionInt(38)),
    nary(plus(), variable("x"), lit(ionInt(4))))
}

val s = toyExprConverter.convert(ast)

println(s)

// prints to the console:
// let x = 38 in x + 4
```



#### Using PIG In Your Project

##### Generating Code

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

## License

This project is licensed under the Apache-2.0 License.


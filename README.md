[![Build Status](https://travis-ci.org/partiql/partiql-ir-generator.svg?branch=master)](https://travis-ci.org/partiql/partiql-ir-generator)

# The PartiQL I.R. Generator

The PariQL I.R. Generator (PIG) is a [nanopass](http://nanopass.org/) inspired type domain code generator currently 
targeting Kotlin.

## API Status

PIG is usable now but its API and the API of the generated Kotlin code is under development and might change.

## Definitions

- `type universe`: A collection of type domains.
- `type domain`: A collection of data types, instances of which can be combined together to form a tree.  Also referred 
to simply as "domain".
- `permuted domain`: A type domain that permutes a copy of another domain by removing types, adding types or altering 
sum types, creating a new, independent domain. 
- `data type`: All of Ion's data types plus sum and product types which are composed of other data types.
- `product type`: also known as n-tuple.  Similar to an array, but the elements of the product types may have. 
different data types.  In this respect it is also similar to a struct in C, but the members do not have names. 
- `sum type`: a.k.a. [tagged union](https://en.wikipedia.org/wiki/Tagged_union).
- `record`: a product or variant with named elements.
- `variant`: An element of a sum type which consists of a name and zero or more types.
- `element`: A slot within a product or variant which holds an instance of data of a specific data type.

## The Problem Solved By PIG

Simply put, the goal of the PIG project is to automate the creation and maintenance of the numerous tree data
structures required by PartiQL.  PIG generates Kotlin classes to represent data type in a domain and generates:

- One Kotlin class per node type.
- `.equals` and `hashCode` implementations
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
// Define type domain for a language named "toy".  
// Toy has literals, variables, basic arithmetic and functions that accept a single argument.

(define toy_lang
    (domain 
        (sum expr
            (product lit (value ion))
            (product variable (name symbol))
            (product not (expr expr))
            (product plus (operands (* expr 2)))
            (product minus (operands (* expr 2)))
            (product times (operands (* expr 2)))
            (product divide (operands (* expr 2)))
            (product modulo (operands (* expr 2)))
            (product call (func expr) (arg expr))
            (product let (name symbol) (value expr) (body expr))
            (product function (arg_name symbol) (body (expr))))

// Define another type domain which is the same as "toy_lang" but replaces variable names with DeBruijn indices:

(define toy_lang_nameless
    (permute_domain toy_lang
        (with expr
            (exclude variable let)
            (include 
                (product variable (index int))
                (product let (index int) (value expr) (body expr))))))
```

### Generated Kotlin Domain Model Sample

See the sample generated code [here](pig-tests/src/org/partiql/pig/tests/generated/sample-universe.kt)

```

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
type_universe ::= '(' 'define' symbol <domain_definition> ')'...

// Domain
domain_definition ::= <domain> | <permute_domain>
domain ::= '(' 'domain' <type_definition>... ')'
type_definition ::= <tuple> | <sum>

// Tuples
tuple ::= '(' ('product' | 'record') symbol <tuple_element>...')'
tuple_element ::= '(' symbol <type_ref> ')'

// Sum
sum ::= '(' 'sum' symbol <tuple>...')'

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
            | '(' 'include' ( '(' <tuple> ')' )... ')' 
        )...
    ')'

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
- Check for errors in type domain (undefined names, etc)
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
| `ion`        | `IonElement` |

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
            (record select
                (project projection)
                (from from_source)
                (where (? expr))
                (group_by (? group_specificaiton))
                (having (? expr))
                (limit (? expr)))
            ...)
        ...)))
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


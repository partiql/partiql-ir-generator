# PartiQL IR Generator

PIG is a compiler framework, domain modeling tool and code generator for tree data structures such as ASTs (Abstract
Syntax Tree), database logical plans, database physical plans, and other intermediate representations. Using PIG, the
developer concisely defines the structure of a tree by specifying named constraints for every node and its attributes.

PIG is mature, but its API and the API of the generated Kotlin code is under active development and may change.

[![PIG Generator](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-ir-generator/badge.svg?)](https://search.maven.org/artifact/org.partiql/partiql-ir-generator)
[![PIG Runtime](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-ir-generator-runtime/badge.svg?)](https://search.maven.org/artifact/org.partiql/partiql-ir-generator-runtime)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/partiql/partiql-ir-generator/blob/main/LICENSE)
[![CI Build](https://github.com/partiql/partiql-ir-generator/actions/workflows/build.yml/badge.svg)](https://github.com/partiql/partiql-ir-generator/actions?query=workflow%3A%22Build+and+run+tests%22)

## About

Check out the wiki! — [PartiQL IR Generator Wiki](https://github.com/partiql/partiql-ir-generator/wiki)

## Usage

There are two components of PIG to be aware of
- [PIG Generator](https://search.maven.org/artifact/org.partiql/partiql-ir-generator)
- [PIG Runtime](https://search.maven.org/artifact/org.partiql/partiql-ir-generator-runtime)

Both are available in [Maven Central](https://search.maven.org/search?q=partiql-ir-generator), and it is recommended that **you use the same version for both of them.**

### Gradle

```groovy
plugins {
    id 'pig-gradle-plugin'
}

sourceSets {
    main {
        pig {
            // in addition to the default 'src/main/pig'
            srcDir 'path/to/type/universes'
        }
    }
    test {
        pig {
            // in addition to the default 'src/test/pig'
            srcDir 'path/to/test/type/universes'
        }
    }
}

pig {
  target = 'kotlin' // required
  namespace = ... // optional
  template = ... // optional
  outDir = 'build/generated-sources/pig/' // default
}
```

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

### Obtaining the PIG Executable

To obtain the `pig` executable:

- Clone this repository.
- Check out the tag of the [release](https://github.com/partiql/partiql-ir-generator/releases) you wish to utilize,
  e.g. `git checkout v0.6.0`
- Execute `./gradlew assemble`

After the build completes, the `pig` executable and dependencies will be located
in `pig/build/distributions/pig/pig-x.y.z.[tar.gz|zip]`.

**Finally, make sure that the version of the `partiql-ir-generator-runtime` library that you are using corresponds to
the version of the executable.**

Verify this with the `--version` command line option of PIG.

## License

This project is licensed under the Apache-2.0 License.


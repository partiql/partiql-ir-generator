The `.ion` files in this directory are used to test various aspects of the `include_file` statement.  Parsing `main.ion`
and the files it includes is challenging if we can do it correctly then we should be able to handle any 
`include_file` statement the user throws at us.

The following concerns of `IncludeCycleHandler`, `IncludeResolver`, and `TypeDomainParser` are being tested here:

- Cyclic includes are tolerated without exception.
- Aboslute and relative paths are respected during include resolution.
- Searching across multiple search roots works.

Also, the concern of the `TypeDomainSemanticChecker` is tested as well:

- Duplicate domains names are detected even if they are defined in different files.
  NOTE: this is a concern of the semantic checker!


├── duplicate_domains.ion                       used by `TypeDomainSemanticCheckerTests`
├── include-missing-absolute.ion                used by parser error handling tests and not included by main.ion
├── include-missing-relative.ion                used by parser error handling tests and not included by main.ion
├── main.ion                                    includes sibling-of-main.ion, root_a/dir_x/universe_a.ion, root_b/dir_z/circular_universe_c.ion
├── root_a
│   └── dir_x
│       ├── circular_universe_c.ion             includes circular_universe_d.ion
│       ├── circular_universe_d.ion             includes circular_universe_c.ion
│       ├── first_duplicated_domain_name.ion    defines domain_dup
│       ├── include_b.ion                       includes . ./dir_a/universe_b
│       └── universe_a.ion                      includes include_b.ion
├── root_b
│   └── dir_z
│       ├── second_duplicated_domain_name.ion   defines domain_dup
│       └── universe_b.ion                      includes nothing
└── sibling-of-main.ion


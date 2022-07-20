[#---------------------------------------------------------------------------------------------------

  This file is the entry point template that generates all of the
  <Domain>To<Domain>VisitorTransform.generated.kt files.

-----------------------------------------------------------------------------------------------------]

[#-- include the #macro definitions that we need below. --]
[#include "kotlin-visitor-transform.ftl"]


[#-- emits the standard header for all of our generated files--]
[#include "kotlin-header.ftl"]

[@visitor_transform_class
    "${transform.sourceDomainDifference.kotlinName}To${transform.destDomainKotlinName}VisitorTransform"
    transform.sourceDomainDifference
    transform.destDomainKotlinName /]

[#ftl output_format="Text"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]


[#--------------------------------------------------------------
    InspectingVisitor/InspectingWalker

  Note that https://youtrack.jetbrains.com/issue/KT-4779
  prevents Java from seeing default visitor implementations
  on interfaces, which is why this is an open class instead
  of an interface.
----------------------------------------------------------------]

open class InspectingVisitor : InspectingDomainVisitorBase() {
    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    open fun visit${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}) { }
    [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun visit${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}) { }
[#list s.variants as t]
    open fun visit${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}) { }
[/#list]
    [/#list]
}

[#macro tuple_walker_body t visitSuffix]
[@indent count=4]
    visitor.visit${visitSuffix}(node)
[#list t.properties as p]
[#if p.variadic]
    node.${p.kotlinName}.map { walk${p.rawTypeName}(it) }
[#elseif p.nullable]
    node.${p.kotlinName}?.let { walk${p.rawTypeName}(it) }
[#else]
    walk${p.rawTypeName}(node.${p.kotlinName})
[/#if]
[/#list]
    walkMetas(node.metas)
[/@indent]
[/#macro]
open class InspectingWalker(
    visitor: ${domain.kotlinName}.InspectingVisitor
) : InspectingDomainWalkerBase<InspectingVisitor>(visitor) {

    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    open fun walk${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}) {
        [@tuple_walker_body t t.kotlinName/][#t]
    }

    [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun walk${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}) {
        visitor.visit${s.kotlinName}(node)
        when(node) {
        [#list s.variants as v]
            is ${domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> walk${s.kotlinName}${v.kotlinName}(node)
        [/#list]
        }
    }

[#list s.variants as t]
    open fun walk${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}) {
        [@tuple_walker_body t "${s.kotlinName}${t.kotlinName}"/]
    }

[/#list]
[/#list]
}

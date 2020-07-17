[#ftl output_format="plainText"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]
[#--------------------------------------------------------------
    FoldingVisitor/FoldingWalker

  Note that https://youtrack.jetbrains.com/issue/KT-4779
  prevents Java from seeing default visitor implementations
  on interfaces, which is why this is an open class instead
  of an interface.
----------------------------------------------------------------]
open class FoldingVisitor<T> : FoldingDomainVisitorBase<T>() {
    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
        [#items as t]
    open fun visit${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}, accumulator: T): T = accumulator
        [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun visit${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}, accumulator: T): T = accumulator
        [#list s.variants as t]
    open fun visit${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}, accumulator: T): T = accumulator
        [/#list]
    [/#list]
}

[#macro tuple_walker_body t visitSuffix]
    [@indent count=4]
    var current = accumulator
    current = visitor.visit${visitSuffix}(node, current)
        [#list t.properties as p]
            [#if p.variadic]
    node.${p.kotlinName}.map { current = walk${p.rawTypeName}(it, current) }
            [#elseif p.nullable]
    node.${p.kotlinName}?.let { current = walk${p.rawTypeName}(it, current) }
            [#else]
    current = walk${p.rawTypeName}(node.${p.kotlinName}, current)
            [/#if]
        [/#list]
    current = walkMetas(node.metas, current)
    return current
    [/@indent]
[/#macro]

open class FoldingWalker<T>(
    visitor: ${domain.kotlinName}.FoldingVisitor<T>
) : FoldingDomainWalkerBase<FoldingVisitor<T>, T>(visitor) {

    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    open fun walk${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}, accumulator: T): T {
        [@tuple_walker_body t t.kotlinName/][#t]
    }

        [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun walk${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}, accumulator: T): T {
        val current = visitor.visit${s.kotlinName}(node, accumulator)
        return when(node) {
        [#list s.variants as v]
            is ${domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> walk${s.kotlinName}${v.kotlinName}(node, current)
        [/#list]
        }
    }

        [#list s.variants as t]
    open fun walk${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}, accumulator: T): T {
        [@tuple_walker_body t "${s.kotlinName}${t.kotlinName}"/]
    }

        [/#list]
    [/#list]
}

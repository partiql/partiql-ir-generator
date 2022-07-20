[#ftl output_format="plainText"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]

[#macro tuple_visitor_fold_walker_body t visitSuffix]
    [@indent count=4]
    var current = accumulator
    current = visit${visitSuffix}(node, current)
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

[#macro visitor_fold_class]
open class VisitorFold<T> : DomainVisitorFoldBase<T>() {
    ////////////////////////////////////////////////////////////////////////////
    // Visit Functions
    ////////////////////////////////////////////////////////////////////////////

    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
        [#items as t]
    open protected fun visit${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}, accumulator: T): T = accumulator
        [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open protected fun visit${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}, accumulator: T): T = accumulator
        [#list s.variants as t]
    open protected fun visit${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}, accumulator: T): T = accumulator
        [/#list]
    [/#list]

    ////////////////////////////////////////////////////////////////////////////
    // Walk Functions
    ////////////////////////////////////////////////////////////////////////////

    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    open fun walk${t.kotlinName}(node: ${domain.kotlinName}.${t.kotlinName}, accumulator: T): T {
        [@tuple_visitor_fold_walker_body t t.kotlinName/][#t]
    }

        [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun walk${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}, accumulator: T): T {
        val current = visit${s.kotlinName}(node, accumulator)
        return when(node) {
        [#list s.variants as v]
            is ${domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> walk${s.kotlinName}${v.kotlinName}(node, current)
        [/#list]
        }
    }

        [#list s.variants as t]
    open fun walk${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}, accumulator: T): T {
        [@tuple_visitor_fold_walker_body t "${s.kotlinName}${t.kotlinName}"/]
    }

        [/#list]
    [/#list]
}
[/#macro]
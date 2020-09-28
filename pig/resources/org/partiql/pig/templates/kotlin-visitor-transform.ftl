[#ftl output_format="plainText"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]

[#macro transform_fun_body destDomainName t transformFuncName]
    [#list t.properties as p]
        val new_${p.kotlinName} = transform${transformFuncName}_${p.kotlinName}(node)
    [/#list]
        val new_metas = transform${transformFuncName}_metas(node)
        return ${destDomainName}.${t.constructorName}(
                [#list t.properties as p]
                ${p.kotlinName} = new_${p.kotlinName},
                [/#list]
                metas = new_metas
            )
[/#macro]

[#macro transform_property_functions t sumName]
[#assign qualifed_name][#if sumName?has_content]${sumName}.[/#if]${t.kotlinName}[/#assign]
[#list t.properties as p]
    open fun transform${sumName}${t.kotlinName}_${p.kotlinName}(node: ${qualifed_name}) =
        [#if p.variadic]
        node.${p.kotlinName}.map { transform${p.rawTypeName}(it) }
        [#elseif p.nullable]
        node.${p.kotlinName}?.let { transform${p.rawTypeName}(it) }
        [#else]
        transform${p.rawTypeName}(node.${p.kotlinName})
        [/#if]
[/#list]
    open fun transform${sumName}${t.kotlinName}_metas(node: ${qualifed_name}) =
        transformMetas(node.metas)

[/#macro]

[#macro visitor_transform_class source_domain destDomainName]
[#if source_domain.transform]abstract[#else]open[/#if] class VisitorTransformTo${destDomainName} : DomainVisitorTransformBase() {
    [#list source_domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    // Tuple ${t.kotlinName}
    [#if !t.removed]
    open fun transform${t.kotlinName}(node: ${t.kotlinName}): ${destDomainName}.${t.kotlinName} {
        [@transform_fun_body destDomainName t t.kotlinName /]
    }
    [@transform_property_functions t ""/]
    [#else]
    abstract fun transform${t.kotlinName}(node: ${t.kotlinName}): ${source_domain.kotlinName}.${t.kotlinName}
    [/#if]
    [/#items]
    [/#list]
    [#list source_domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun transform${s.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}): ${destDomainName}.${s.kotlinName} =
        when(node) {
        [#list s.variants as v]
            is ${source_domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> transform${s.kotlinName}${v.kotlinName}(node)
        [/#list]
        }
[#list s.variants as t]
    // Variant ${s.kotlinName}${t.kotlinName}
    [#if !t.removed]
    open fun transform${s.kotlinName}${t.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}.${t.kotlinName}): ${destDomainName}.${s.kotlinName}  {
        [@transform_fun_body destDomainName, t, "${s.kotlinName}${t.kotlinName}" /]
    }
    [@transform_property_functions t s.kotlinName /]
    [#else]
    abstract fun transform${s.kotlinName}${t.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}.${t.kotlinName}): ${destDomainName}.${s.kotlinName}
    [/#if]
[/#list]
[/#list]
}
[/#macro]

[@visitor_transform_class domain domain.kotlinName/]

[#list domain.transforms]
//////////////////////////////////////
// Transforms to other domains
//////////////////////////////////////
[#items as t]
[@visitor_transform_class t.sourceDomainWithRemovals t.destDomainName/]
[/#items]
[/#list>

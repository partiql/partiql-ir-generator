[#ftl output_format="plainText"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]

[#macro transform_fun_body destDomainName t transformFuncName avoidCopyingUnchangedNodes]
    [#list t.properties as p]
        val new_${p.kotlinName} = transform${transformFuncName}_${p.kotlinName}(node)
    [/#list]
        val new_metas = transform${transformFuncName}_metas(node)
        return [#if avoidCopyingUnchangedNodes]if (
            [#list t.properties as p]
            node.${p.kotlinName} !== new_${p.kotlinName} ||
            [/#list]
            node.metas !== new_metas
        ) {
            ${destDomainName}.${t.constructorName}(
                [#list t.properties as p]
                ${p.kotlinName} = new_${p.kotlinName},
                [/#list]
                metas = new_metas
            )
        } else {
            node
        }
    [#else][#t]
        ${destDomainName}.${t.constructorName}([#lt]
        [#list t.properties as p]
            ${p.kotlinName} = new_${p.kotlinName},
        [/#list]
            metas = new_metas
        )
    [/#if]
[/#macro]

[#macro transform_property_functions source_domain tuple sumName]
[#assign qualifed_name]${source_domain.kotlinName}.[#if sumName?has_content]${sumName}.[/#if]${tuple.kotlinName}[/#assign]
[#list tuple.properties as p]
    open fun transform${sumName}${tuple.kotlinName}_${p.kotlinName}(node: ${qualifed_name}) =
        [#if p.variadic]
        node.${p.kotlinName}.map { transform${p.rawTypeName}(it) }
        [#elseif p.nullable]
        node.${p.kotlinName}?.let { transform${p.rawTypeName}(it) }
        [#else]
        transform${p.rawTypeName}(node.${p.kotlinName})
        [/#if]
[/#list]
    open fun transform${sumName}${tuple.kotlinName}_metas(node: ${qualifed_name}) =
        transformMetas(node.metas)

[/#macro]

[#macro visitor_transform_class class_name source_domain dest_domain_name]
abstract class ${class_name} : DomainVisitorTransformBase() {
    [#list source_domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as tuple]
    [#if !tuple.transformAbstract]
    // Tuple ${tuple.kotlinName}
    open fun transform${tuple.kotlinName}(node: ${source_domain.kotlinName}.${tuple.kotlinName}): ${dest_domain_name}.${tuple.kotlinName} {
        [@transform_fun_body dest_domain_name tuple tuple.kotlinName source_domain.kotlinName == dest_domain_name/]
    }
    [@transform_property_functions source_domain tuple ""/]
    [#else]
    abstract fun transform${tuple.kotlinName}(node:${source_domain.kotlinName}.${tuple.kotlinName}): ${dest_domain_name}.${tuple.kotlinName}
    [/#if]
    [/#items]
    [/#list]
    [#list source_domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun transform${s.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}): ${dest_domain_name}.${s.kotlinName} =
        when(node) {
        [#list s.variants as v]
            is ${source_domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> transform${s.kotlinName}${v.kotlinName}(node)
        [/#list]
        }
[#list s.variants as tuple]
    // Variant ${s.kotlinName}${tuple.kotlinName}
    [#if !tuple.transformAbstract]
    open fun transform${s.kotlinName}${tuple.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}.${tuple.kotlinName}): ${dest_domain_name}.${s.kotlinName} {
        [@transform_fun_body dest_domain_name, tuple, "${s.kotlinName}${tuple.kotlinName}" source_domain.kotlinName == dest_domain_name/]
    }
    [@transform_property_functions source_domain tuple s.kotlinName /]
    [#else]
    abstract fun transform${s.kotlinName}${tuple.kotlinName}(node: ${source_domain.kotlinName}.${s.kotlinName}.${tuple.kotlinName}): ${dest_domain_name}.${s.kotlinName}
    [/#if]
[/#list]
[/#list]
}
[/#macro]


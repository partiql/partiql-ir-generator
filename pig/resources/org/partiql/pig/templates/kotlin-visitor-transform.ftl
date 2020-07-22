[#ftl output_format="plainText"]
[#-- @ftlvariable name="universe" type="org.partiql.pig.generator.kotlin.KTypeUniverse" --]

[#macro transform_fun_body domain t transformFuncName]
    [#list t.properties as p]
        val new_${p.kotlinName} = transform${transformFuncName}${p.kotlinNamePascalCased}(node)
    [/#list]
        val new_metas = transform${transformFuncName}Metas(node)
        return build {
            ${t.constructorName}(
                [#list t.properties as p]
                ${p.kotlinName} = new_${p.kotlinName},
                [/#list]
                metas = new_metas
            )
        }
[/#macro]

[#macro trasnform_property_functions t sumName]
[#assign qualifed_name][#if sumName?has_content]${sumName}.[/#if]${t.kotlinName}[/#assign]
[#list t.properties as p]
    open fun transform${sumName}${t.kotlinName}${p.kotlinNamePascalCased}(node: ${qualifed_name}) =
        [#if p.variadic]
        node.${p.kotlinName}.map { transform${p.rawTypeName}(it) }
        [#elseif p.nullable]
        node.${p.kotlinName}?.let { transform${p.rawTypeName}(it) }
        [#else]
        transform${p.rawTypeName}(node.${p.kotlinName})
        [/#if]
[/#list]
    open fun transform${sumName}${t.kotlinName}Metas(node: ${qualifed_name}) =
        transformMetas(node.metas)

[/#macro]


open class VisitorTransform : DomainVisitorTransformBase() {
    [#list domain.tuples]
    //////////////////////////////////////
    // Tuple Types
    //////////////////////////////////////
    [#items as t]
    // Tuple ${t.kotlinName}
    open fun transform${t.kotlinName}(node: ${t.kotlinName}): ${t.kotlinName} {
        [@transform_fun_body domain t t.kotlinName /]
    }
    [@trasnform_property_functions t ""/]
    [/#items]
    [/#list]
    [#list domain.sums as s]
    //////////////////////////////////////
    // Sum Type: ${s.kotlinName}
    //////////////////////////////////////
    open fun transform${s.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}) =
        when(node) {
        [#list s.variants as v]
            is ${domain.kotlinName}.${s.kotlinName}.${v.kotlinName} -> transform${s.kotlinName}${v.kotlinName}(node)
        [/#list]
        }
[#list s.variants as t]
    // Variant ${s.kotlinName}${t.kotlinName}
    open fun transform${s.kotlinName}${t.kotlinName}(node: ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName}): ${domain.kotlinName}.${s.kotlinName}.${t.kotlinName} {
        [@transform_fun_body domain, t, "${s.kotlinName}${t.kotlinName}" /]
    }

    [@trasnform_property_functions t s.kotlinName /]

[/#list]
[/#list]
}

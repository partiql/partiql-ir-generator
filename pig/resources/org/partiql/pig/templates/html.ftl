[#ftl output_format="XHTML"]
<!DOCTYPE html>
<html lang="en_US">
<head>
    <style type="text/css">
        table, th, td {
            border: 1px solid black;
            padding: 3px
        }
        th {
            text-align: left;
        }
        td {
            vertical-align: top;
        }
    </style>
    <title>Type Universe</title>

</head>
<body>
<h1>
    Domains
</h1>

[#macro elements_table tuple]
<table style="width:100%">
    <thead>
    <tr>
        <th>
            Name
        </th>
        <th>
            Type
        </th>
        <th>
            Optional
        </th>
        <th>
            Variadic
        </th>
    </tr>
    </thead>
    <tbody>
    [#list tuple.elements as element]
        <tr>
            <td>
                ${element.name}
            </td>
            <td>
                ${element.type}
            </td>
            <td>
                ${element.optional?c}
            </td>
            <td>
                ${element.variadic?c}
            </td>
        </tr>
    [/#list]
    </tbody>
</table>
[/#macro]

[#macro table_of_tuples tuples]
    <table>
        <thead>
            <tr>
                <th>
                    Tag Name
                </th>
                <th>
                    Tuple Type
                </th>
                <th>
                    Elements
                </th>
            </tr>
        </thead>
        <tbody>
            [#list tuples as tuple]
                <tr>
                    <td>
                        ${tuple.name}
                    </td>
                    <td>
                        ${tuple.tupleType}
                    </td>
                    <td>
                        [@elements_table tuple/]
                    </td>
                </tr>
            [/#list]
        </tbody>
    </table>
[/#macro]

[#list domains as domain]
    <h2>
        ${domain.name}
    </h2>
    <h3>
        Product Types
    </h3>
    [#if domain.tuples?size == 0]
        <i>This domain has no product types.</i>
    [#else]
        [@table_of_tuples domain.tuples/]
    [/#if]
    <h3>
        Sum Types
    </h3>
    [#if domain.sums?size == 0]
        <i>This domain has no sum types.</i>
    [#else]
        <table>
            <thead>
                <tr>
                    <th>
                        Tag Name
                    </th>
                    <th>
                        Variants
                    </th>
                </tr>
            </thead>
            <tbody>
                [#list domain.sums as sum]
                    <tr>
                        <td>
                            ${sum.name}
                        </td>
                        <td>
                            [@table_of_tuples sum.variants /]
                        </td>
                    </tr>
                [/#list]
            </tbody>
        </table>
    [/#if]
[/#list]
</body>
</html>


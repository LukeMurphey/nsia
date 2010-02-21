<#macro formtable fields>
    <#list fields.layout as row >
    <#list row.fields as field >
    <tr class="<#if (form_errors?? && form_errors.fieldHasError(field.name) )>ValidationFailed<#else>Background1</#if>">     
        <td class="Text_3" colspan="${field.layoutWidth?c}">
            <div style="display: table-cell; vertical-align: top">${field.title?html}</div>
        </td>
        <#if ( field.type = "file") >
        <td colspan="${field.layoutWidth?c}">
            <input enctype="multipart/form-data" style="width: 400px" type="file" name="_${field.name?html}">
        </td>
        <#elseif ( field.type = "text" )>
            <#if ( field.height > 1 )>
        <td colspan="${field.layoutWidth?c}">
            <textarea rows="${field.height?c}" style="width: 400px" name="_${field.name?html}"><#if field.defaultValue?? >${field.defaultValue?html}</#if></textarea>
        </td>
            <#else>
        <td colspan="${field.layoutWidth?c}">
            <input class="textInput" style="width: 400px" type="text" name="_${field.name}" <#if field.defaultValue?? >value="${field.defaultValue?html}"</#if>>
        </td>
            </#if>
        <#elseif ( field.type = "password" ) >
        <td colspan="${field.layoutWidth?c}">
            <input class="textInput" style="width: 400px" type="password" name="_${field.name}" <#if field.defaultValue?? >value="${field.defaultValue?html}"</#if>>
        </td>
        <#else>
        <td colspan="${field.layoutWidth?c}">
            <input class="textInput" style="width: 400px" type="text" name="_${field.name}" <#if field.defaultValue?? >value="${field.defaultValue?html}"</#if>>
        </td>
        </#if>
    </tr>
    </#list>
    </#list>
</#macro>
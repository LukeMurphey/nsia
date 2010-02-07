<#function min x y>
    <#assign result>
        <#if (x < y)>${x?c}<#else>${y?c}</#if>
    </#assign>
    <#return result />
</#function>
<#function max x y>
    <#assign result>
        <#if (x > y)>${x?c}<#else>${y?c}</#if>
    </#assign>
    <#return result />
</#function>
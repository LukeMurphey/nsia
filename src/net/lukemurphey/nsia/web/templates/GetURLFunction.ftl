<#function geturl name args...>
    <#assign result>
        <#if (args?size > 0)>
            <@url name=name args=args />
        <#else>
            <@url name=name />
        </#if>
    </#assign>
  <#return result />
</#function>
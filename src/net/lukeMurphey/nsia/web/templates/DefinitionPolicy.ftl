<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#macro policyrow category >
    <tr>
    <#if category.enabled >
        <td width="40" align="center" class="StatGreen"><img src="/media/img/22_Check" alt="ok"></td>
    <#else>
        <td width="40" align="center" class="StatRed"><img src="/media/img/22_Alert" alt="alert"></td>
    </#if>
        <td width="6"><input type="checkbox" name="DefinitionPolicy" value="${category.name?html}"></td>
        <td>${category.name}</td>
        
    <#if category.enabled >
        <td>Enabled
    <#else>
        <td>Disabled
    </#if>
        
    <#if (sitegroup?? && category.default) >
         (inherited from <a href="<@url name="definitions_policy" />">default policy</a>)
    </#if>
        </td>
    </tr>
</#macro>

<#assign content>
<span class="Text_1">Scan Policy Management</span>
<#if (sitegroup??)>
<br><span class="LightText">Viewing scan policy for Site Group ${sitegroup.groupName}</span>
<#else>
<br><span class="LightText">Viewing default scan policy</span>
</#if>
<p>

<form action="${request.thisURL}" method="post" action="ScanPolicy">
<#if (sitegroup??)>
    <input type="hidden" name="SiteGroupID" value="${sitegroup.groupId}">
</#if>
<#if categories?size = 0 >

<#else>
    <table class="DataTable" width="80%" summary="Definition categories">
        <thead>
            <tr>
                <td colspan="99">Category</td>
            </tr>
        </thead>
    <#list categories as category>
        <@policyrow category=category />
    </#list>
</#if>

        <tr class="lastRow">
            <td colspan="99">
                <input type="submit" class="button" name="Action" value="Disable">&nbsp;<input type="submit" class="button" name="Action" value="Enable">
            
        <#if sitegroup??>
            &nbsp;<input type="submit" class="button" name="Action" value="Set Default">
        </#if>
            </td>
        </tr>            
    </table>
</form>
</#assign>
<#include "BaseWithNav.ftl">
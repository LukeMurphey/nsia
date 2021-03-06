<#include "GetDialog.ftl">
<#assign content>
<#macro statusrow htmlclass icon title message link>
        <table cellspacing="0" cellpadding="0" width="95%">
            <tr class="BorderRow">
                <td align="center" width="48" height="48" class="${htmlclass}">
                    <img src="${icon}" alt="StatusIcon">
                </td>
                <td class="StatGray" style="border-left: 0px; border-right: 0px;" width="16">&nbsp;</td>
                <td class="StatGray" style="border-left: 0px; border-right: 0px;">
                    <#if link??><a href="${link}"></#if><span class="Text_3">${title?html}</span><#if link??></a></#if>
                    <br>${message?html}</td>
                <td width="16" class="${htmlclass}">&nbsp;</td>
            </tr>
         </table>
         <br>
</#macro>

<span class="Text_2">System Status</span>
<#if system_status.overallStatus == 0>
    <#assign htmlclass="StatGreen" icon="/media/img/22_Check">
<#elseif system_status.overallStatus == 1>
    <#assign htmlclass="StatYellow" icon="/media/img/22_Warning">
<#else>
    <#assign htmlclass="StatRed" icon="/media/img/22_Alert">
</#if>
<#assign link><@url name="system_status" /></#assign>
<@statusrow htmlclass=htmlclass icon=icon message="${system_status.longDescription?html}" title="Operational Status" link=link />
<div style="height: 16px"></div>

<#if (sitegroups?? && (sitegroups?size > 0))>
<span class="Text_2">Scan Results</span>
<#list sitegroups as sitegroup>
    <#if sitegroup.deviatingRules == 1>
        <#assign htmlclass="StatRed" message="1 rule has rejected" icon="/media/img/22_Alert">
    <#elseif (sitegroup.deviatingRules > 1) >
        <#assign htmlclass="StatRed" message="${sitegroup.deviatingRules} rules have rejected" icon="/media/img/22_Alert">
    <#elseif sitegroup.incompleteRules == 1>
        <#assign htmlclass="StatYellow" message="1 rule has failed to scan completely" icon="/media/img/22_Warning">
    <#elseif (sitegroup.incompleteRules > 1) >
        <#assign htmlclass="StatYellow" message="${sitegroup.incompleteRules} rule has failed to scan completely" icon="/media/img/22_Warning">
    <#else>
        <#assign htmlclass="StatGreen" message="0 rules have rejected" icon="/media/img/22_Check">
    </#if>
    
    <#if sitegroup.siteGroupDescriptor.enabled == false >
        <#assign icon=icon + "Disabled" htmlclass="StatGrayDark">
    </#if>
    <#assign link><@url name="sitegroup_rules" args=[sitegroup.siteGroupId] /></#assign>
    <@statusrow htmlclass=htmlclass icon=icon message=message title="${sitegroup.siteGroupDescriptor.groupName?html}" link=link />
</#list>

<table style="margin-bottom: 16px">
    <tr>
        <td width="4"></td>
        <td><img src="/media/img/16_Add"></td>
        <td><a href="<@url name="sitegroup" args=["New"] />">[Create another site-group]</a></td>
    </tr>
</table>
<#else>
	<#assign message>
	No resources are being monitored yet. Create a site-group and define a rule to begin monitoring.
	<p><a href="<@url name="sitegroup" args=["New"] />">[Create Site-group Now]</a></#assign>
	<@getinfodialog title="No Monitored Resources" message=message />
</#if>
</#assign>

<#include "BaseWithNav.ftl">
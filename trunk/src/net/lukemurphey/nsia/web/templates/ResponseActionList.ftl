<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>

<#if (actions?? && actions?size > 0)>
    <span class="Text_1">
    Incident Response Actions
            
    <#if ( isSiteGroup?? && isSiteGroup )>
    <form method="post" action="Response"><input type="hidden" name="SiteGroupID" value="${siteGroup.groupId}">
    </#if>
    
    </span><p/>
        <table width="600px" class="DataTable" summary="List of Response Actions">
            <thead>
                <tr>
                    <td colspan="2">Type</td>
                    <td colspan="2">Description</td>
                </tr>
            </thead>
            <tbody>
            <#list actions as action>
                <tr>
                    <td width="2px"><input name="ActionID" value="${action.eventLogHookID}" type="checkbox"/></td>
                    <td>${action.action.description}</td>
                    <td>${action.action.configDescription}</td>
                    <td>
                        <table>
                            <tr>
                                <td><a href="/Response?Action=Edit&ActionID=${action.eventLogHookID}"><img class="imagebutton" src="/media/img/16_Configure"/></a></td>
                                <td><a href="/Response?Action=Edit&ActionID=${action.eventLogHookID}">Edit</a></td>
                            </tr>
                        </table>
                    </td>
                 </tr>
            </#list>
                <tr>
                    <td colspan="99"><input class="button" name="Action" value="Delete" type="submit"></td>
                </tr>
            </tbody>
       </table>
   </form>
<#else>
    <@getinfodialog title="No Actions Exist" message="No actions exist yet for the given site-group<br><a href=\"Response?SiteGroupID=${siteGroup.groupId}&Action=New\">[create a new action now]</a>" />
</#if>

</#assign>
<#include "BaseWithNav.ftl">
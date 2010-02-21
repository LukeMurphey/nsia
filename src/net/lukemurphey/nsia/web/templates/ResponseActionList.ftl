<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>

<#if (actions?? && actions?size > 0)>
    <span class="Text_1">Incident Response Actions</span>
    <#if ( isSiteGroup?? && isSiteGroup )>
    <form method="post" action="Response"><input type="hidden" name="SiteGroupID" value="${siteGroup.groupId?c}">
    <#else>
    <form method="post" action="<@url name="sitegroup_actions" args=[siteGroup.groupId] />">
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
                    <td width="2px"><input name="ActionID" value="${action.eventLogHookID?c}" type="checkbox"/></td>
                    <td>${action.action.description?html}</td>
                    <td>${action.action.configDescription?html}</td>
                    <td>
                        <table>
                            <tr>
                                <td><a href="<@url name="sitegroup_action_editor" args=["Edit", action.eventLogHookID]/>"><img class="imagebutton" src="/media/img/16_Configure"/></a></td>
                                <td><a href="<@url name="sitegroup_action_editor" args=["Edit", action.eventLogHookID]/>">Edit</a></td>
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
    <#assign message>No actions exist yet for the given site-group<br><a href="<@url name="sitegroup_action_editor" args=["New"] />?SiteGroupID=${siteGroup.groupId?c}">[create a new action now]</a></#assign>
    <@getinfodialog title="No Actions Exist" message=message />
</#if>

</#assign>
<#include "BaseWithNav.ftl">
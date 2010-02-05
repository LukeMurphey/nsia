<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Shortcuts.ftl">
<#assign content>
<span class="Text_1">Group Management</span><br>&nbsp;

<#if (!groups?? | groups?size = 0)>
    <#assign message>
        No groups exist yet. <p><a href="<@url name="user_editor" args=["New"] />">[Create Group Now]</a>
    </#assign>
    <@getinfodialog message=message title="No Groups Exist Yet" />
<#else>
     <table>
        <tr class="Background0">
            <td class="Text_3">Group ID</td>
            <td class="Text_3">Group Name</td>
            <td class="Text_3">Status</td>
            <td class="Text_3">Group Description</td>
        </tr>
        <#list groups as group>
        <tr class="Background1">
            <td>${group.groupId}</td>
            <td><a href="GroupManagement?GroupID=${group.groupId}">
                <#if group.groupState = ACTIVE >
                    <@iconlink url=geturl("group_editor", "Edit", group.groupId) imageURL="/media/img/16_Group" imageAlt="Enabled" text="${group.groupName}" />
                <#else>
                    <@iconlink url=geturl("group_editor", "Edit", group.groupId) imageURL="/media/img/16_GroupDisabled" imageAlt="Disabled" text="${group.groupName}" />
                </#if>
            </td>
            <td><#if group.groupState = ACTIVE >Enabled<#else>Disabled</#if>&nbsp;&nbsp;</td>
            <td class="Background1"><#if (group.description?? & group.description?length > 0 )><@truncate_chars length=64>${group.description}</@truncate_chars><#else>(No Description Specified)</#if></td>
        </tr>
        </#list>
     </table>
</#if>
</#assign>
<#include "BaseWithNav.ftl" />
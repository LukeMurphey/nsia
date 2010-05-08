<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>

    <span class="Text_1">Group Management</span><br />
    <br/>
    
    <form action="<@url name="group_editor" args=["Edit", group.groupId]/>" method="get">
        <table>
            <tr class="Background1">
                <td width="150" class="Text_3">Group ID</td>
                <td width="200">${group.groupId?c}</td>
            </tr>
            <tr class="Background1">
                <td class="Text_3">Name</td>
                <td>${group.groupName?html}</td>
            </tr>
            <#if group.description?? && (group.description?length > 0) >
            <tr class="Background1">
                <td class="Text_3">Description</td>
                <td>${group.description?html}</td>
            </tr>
            <#else>
            <tr class="Background1">
                <td class="Text_3">Description</td>
                <td>(Unspecified)</td>
            </tr>
            </#if>
            
            <tr class="Background3">
                <td colspan="99" align="right">
                    <input class="button" type="submit" value="Edit Group">
                </td>
            </tr>
        </table>
     </form>
     
     <br><span class="Text_1">User Membership</span>
        
     <#if !can_enum_users>
        <tr>
            <td colspan="99">" + Html.getWarningNote( "You do not have permission to enumerate who is in what group") + "</td>
        </tr>
        </table>
     <#else>
        <#if users?size == 0 >
            <#assign message>No users exist yet. Create a user first, then add the user to the group. <p><a href="<@url name="group_editor" args=["New"] />">[Create Group Now]</a></#assign>
            <@getinfodialog title="No Groups Exist" message=message />
        <#else>
            <form method="POST" action="<@url name="group_edit_membership" args=[group.groupId] />">
                <input type="hidden" name="UserID" value="${group.groupId?c}">
                <input type="hidden" name="Action" value="SetGroup">
            
                <table width="600">
                    <tr class="Background0">
                        <td width="200" class="Text_3">User Name</td>
                        <td class="Text_3">User ID</td>
                        <td width="300" class="Text_3">Full Name</td>
                    </tr>
                    
                <#list users as user>
                    <tr class="Background1">
                        <td>
                            <input id="${user.userID}?c" type="checkbox" name="${user.userID?c}" <#if user.memberOf>checked</#if>>
                            <label for="${user.userID?c}">${user.userName?html}</label>
                            <#if user.enabled >
                                &nbsp;&nbsp;<img alt="Enabled" src="/media/img/16_User">
                            <#else>
                                &nbsp;&nbsp;<img alt="Disabled" src="/media/img/16_UserDisabled">
                            </#if>
                         </td>
                         <td>
                            <a href="<@url name="user" args=[user.userID] />">${user.userID} [View]</a>
                         </td>
                         <td>${user.fullname?html}</td>
                    </tr>
                </#list>
                <tr class="Background3">
                    <td class="alignRight" colspan="3">
                        <input type="hidden" name="IncludedUsers" value="${included_users?html}">
                        <input class="button" type="submit" value="Apply Changes">
                    </td>
                </tr>
              </form>
            </table>
        </#if>
     </#if>
     
</#assign>
<#include "BaseWithNav.ftl">
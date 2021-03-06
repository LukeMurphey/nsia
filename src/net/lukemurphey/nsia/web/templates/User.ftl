<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>

    <span class="Text_1">User Management</span><br />
    <br/>
    
    <form action="<@url name="user_editor" args=["Edit", user.userID]/>" method="get">
        <table>
            <tr class="Background1">
                <td width="150" class="Text_3">User ID</td>
                <td width="200">${user.userID?c}</td>
            </tr>
            <tr class="Background1">
                <td class="Text_3">Username</td>
                <td>${user.userName?html} <#if user.bruteForceLocked><span class="WarnText">(account is locked due to authentication failures,</span> <a href="<@url name="user_unlock" args=[user.userID] />">Unlock now</a><span class="WarnText">)</span></#if></td>
            </tr>
            <#if user.fullname?? >
            <tr class="Background1">
                <td class="Text_3">Full Name</td>
                <td>${user.fullname?html}</td>
            </tr>
            <#else>
            <tr class="Background1">
                <td class="Text_3">Full Name</td>
                <td>(Unspecified)</td>
            </tr>
            </#if>
            
            <#if user.emailAddress??>
            <tr class="Background1">
                <td class="Text_3">Email Address</td>
                <td><a href="mailto:${user.emailAddress?html}">${user.emailAddress?html}</a></td>
            </tr>
            <#else>
            <tr class="Background1">
                <td class="Text_3">Email Address</td>
                <td>(Unspecified)</td>
            </tr>
            </#if>
            <tr class="Background1">
                <td class="Text_3">Account Type</td>
                <td>
                <#if user.unrestricted>
                    <span title="Access controls do not apply to unrestricted users and can therefore perform any operations" class="WarnText">Unrestricted</span>
                <#else>
                    Normal (Restricted)
                </#if>
                </td>
            </tr>
            <tr class="Background3">
                <td colspan="99" align="right">
                    <input class="button" type="submit" value="Edit User">
                </td>
            </tr>
        </table>
     </form>

    <#-- <#if user.bruteForceLocked>
        <#assign message>The account was locked due to repeated and unsuccessful authentication attempts<p/><a href="<@url name="user_unlock" args=[user.userID] />">[Unlock Account]</a></#assign>
        <@getwarndialog title="Account Locked" message=message />
    </#if> -->

     <br><span class="Text_1">Group Membership</span>
        
     <#if !can_enum_groups>
        <tr>
            <td colspan="99">" + Html.getWarningNote( "You do not have permission to enumerate who is in what group") + "</td>
        </tr>
        </table>
     <#else>
        <#if groups?size == 0 >
            <#assign message>No groups exist yet. Create a group first, then add the user to the specified group or groups. <p><a href="<@url name="group_editor" args=["New"] />">[Create Group Now]</a></#assign>
            <@getinfodialog title="No Groups Exist" message=message />
        <#else>
            <#include "SelectAll.ftl">
            <form method="POST" action="<@url name="user_edit_membership" args=[user.userID] />">
                <input type="hidden" name="UserID" value="${user.userID?c}">
                <input type="hidden" name="Action" value="SetGroup">
            
                <table width="600">
                    <tr class="Background0">
                        <td width="200" class="Text_3">
                            <div style="float:left">
                                <input type="checkbox" id="selectall">
                            </div>
                            <div>Group Name</div>
                        </td>
                        <td class="Text_3">GroupID</td>
                        <td width="300" class="Text_3">Group Description</td>
                    </tr>
                    
                <#list groups as group>
                    <tr class="Background1">
                        <td>
                            <input id="${group.ID}" class="selectable" type="checkbox" name="${group.ID}" <#if group.memberOf>checked</#if>>
                            <label for="${group.ID?c}">${group.name?html}</label>
                            <#if group.status = ACTIVE >
                                &nbsp;&nbsp;<img alt="Enabled" src="/media/img/16_Group">
                            <#else>
                                &nbsp;&nbsp;<img alt="Disabled" src="/media/img/16_GroupDisabled">
                            </#if>
                         </td>
                         <td>
                            <a href="<@url name="group" args=[group.ID] />">${group.ID} [View]</a>
                         </td>
                         <td><@truncate_chars length=32>${group.description?html}</@truncate_chars></td>
                    </tr>
                </#list>
                <tr class="Background3">
                    <td class="alignRight" colspan="3">
                        <input type="hidden" name="IncludedGroups" value="${included_groups?html}">
                        <input class="button" type="submit" value="Apply Changes">
                    </td>
                </tr>
              </form>
            </table>
        </#if>
     </#if>
</#assign>
<#include "BaseWithNav.ftl">
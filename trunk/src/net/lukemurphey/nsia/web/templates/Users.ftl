<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#if (users?? & users?size = 0) >
    <#assign message>
        No users exist yet. <p><a href="<@url name="user_editor" args=["New"] />">[Create User Now]</a>
    </#assign>
    <@getinfodialog message=message title="No Users Exist Yet" />
<#else>
<span class="Text_1">Users</span><br>&nbsp;
<table>
    <tr class="Background0">
        <td class="Text_3">User ID</td>
        <td width="270" class="Text_3">Username</td>
        <td width="200" class="Text_3">Full Name</td>
        <td class="Text_3">Status</td>
        <td class="Text_3">Unrestricted</td>
        <td width="200" class="Text_3">Email Address</td>
    </tr>
    <#list users as user>
    <tr class="Background1">
        <td>${user.userID}</td>
        <td><a href="<@url name="user" args=[user.userID]/>">${user.userName}        
        <#-- 1 -- Output the icon associated with the account type -->
        <#if user.unrestricted && user.accountStatus != DISABLED >
        &nbsp;&nbsp;<img src="/media/img/16_Admin" alt="Unrestricted"></a>
        <#elseif user.unrestricted && user.accountStatus = DISABLED >
        &nbsp;&nbsp;<img src="/media/img/16_AdminDisabled" alt="Unrestricted"></a>
        <#elseif user.accountStatus = DISABLED >
        &nbsp;&nbsp;<img src="/media/img/16_UserDisabled" alt="Unrestricted"></a>
        <#else>
        &nbsp;&nbsp;<img src="/media/img/16_User" alt="Restricted"></a>
        </#if>
        <#if user.bruteForceLocked><span class="WarnText">(account is locked)</span></#if></td>
        
        <#if user.fullname?? >
        <td><a href="<@url name="user" args=[user.userID]/>">${user.fullname}</td>
        <#else>
        <td>(Unspecified)</td>
        </#if>
        
        <#-- 3 -- Output the account status -->
        <#if user.accountStatus = ADMINISTRATIVELY_LOCKED >
        <td>Administratively locked</td>
        <#elseif user.bruteForceLocked && user.accountStatus = BRUTE_FORCE_LOCKED >
        <td><span class="WarnText">Brute force locked</span></td>
        <#elseif user.accountStatus = DISABLED >
        <td>Disabled</td>
        <#else>
        <td>Active</td>
        </#if>
        
        <#-- 4 -- Output if the account is restricted -->
        <#if user.unrestricted >
        <td>Unrestricted</td>
        <#else>
        <td>Restricted</td>
        </#if>
        
        <#-- 5 -- Output the email address -->
        <#if user.emailAddress??>
        <td class="Background1"><a href="mailto:${user.emailAddress}">${user.emailAddress}</a></td>
        <#else>
        <td class="Background1">(None Specified)</td>
        </#if>
    </tr>
    </#list>
</table>
</#if>
</#assign>
<#include "BaseWithNav.ftl" />
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <span class="Text_1">Users</span>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <form action="<@url name="user_password" args=[user.userID]/>" method="post" onsubmit="showHourglass('Updating...')">
    <table class="DataTable">
        <tr><#-- Output the username and user ID -->
            <td class="TitleText">User</td>
            <td>${user.userName} (${user.userID})</td>
        </tr>
        <tr class="<#if (form_errors?? && form_errors.fieldHasError("YourPassword"))>ValidationFailed<#else>Background1</#if>"><#-- Existing password field -->
            <td class="TitleText">Your Current Password</td>
            <td><input class="textInput" style="width: 250px;" type="password" name="YourPassword"></td>
        </tr>
        <tr class="<#if (form_errors?? && form_errors.fieldHasError("Password"))>ValidationFailed<#else>Background1</#if>"><#-- New password field -->
            <td class="TitleText">New Password</td>
            <td><input class="textInput" style="width: 250px;" type="password" name="Password"></td>
        </tr>
        <tr class="<#if (form_errors?? && form_errors.fieldHasError("PasswordConfirm"))>ValidationFailed<#else>Background1</#if>"><#-- New password field (confirmation) -->
            <td class="TitleText">Confirm New Password</td>
            <td><input class="textInput" style="width: 250px;" type="password" name="PasswordConfirm"></td>
        </tr>
        <tr class="lastRow">
            <td colspan="2" align="right">
                <input class="button" type="submit" name="Submit" value="Apply">
            </td>
        </tr>
     </table>
     </form>
</#assign>
<#include "BaseWithNav.ftl" />
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <div><span class="Text_1">User Management</span>
    <br><span class="LightText"><#if user??>Edit<#else>Create New</#if> User</span>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <p>
    <table class="DataTable">
        <form onsubmit="showHourglass()" method="post">
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Username"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Username</td>
                <td><input class="textInput" size="32" type="text" name="Username" value="<#if request.getParameter("Username")??>${request.getParameter("Username")?html}<#elseif user??>${user.userName?html}</#if>"></td>
            </tr>
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Fullname"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Full Name</td>
                <td><input class="textInput" size="32" type="text" name="Fullname" value="<#if request.getParameter("Fullname")??>${request.getParameter("Fullname")?html}<#elseif user?? && user.fullname??>${user.fullname?html}</#if>"></td>
            </tr>
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("EmailAddress"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Email Address</td>
                <td><input class="textInput" size="32" type="text" name="EmailAddress" value="<#if request.getParameter("EmailAddress")??>${request.getParameter("EmailAddress")?html}<#elseif user?? && user.emailAddress??>${user.emailAddress?html}</#if>"></td>
            </tr>
            <#if !user??>
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Password"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Password</td><td><input class="textInput" size="32" type="password" name="Password"></td>
            </tr>
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("PasswordConfirm"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Password (Confirm)</td>
                <td><input class="textInput" size="32" type="password" name="PasswordConfirm"></td>
            </tr>
            </#if>
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Unrestricted"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Account Type</td>
                <td><input type="checkbox"<#if request.getParameter("Unrestricted")??> checked<#elseif (user?? && user.unrestricted)> checked</#if> name="Unrestricted">Unrestricted</td>
            </tr>
            <tr class="lastRow">
                <td colspan="2" align="right"><input class="button" type="submit" value="<#if user??>Apply Changes<#else>Create User</#if>"></td>
            </tr>
        </form>
    </table>
</#assign>
<#include "BaseWithNav.ftl">
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
    <div><span class="Text_1">User Management</span>
    <br><span class="LightText">Create New User</span>
    <p>
    <table class="DataTable">
        <form onsubmit="showHourglass()" method="post">
            <tr>
                <td class="Text_3">Username</td>
                <td><input class="textInput" size="32" type="text" name="Username" value=""></td>
            </tr>
            <tr>
                <td class="TitleText">Full Name</td>
                <td><input class="textInput" size="32" type="text" name="Fullname" value=""></td>
            </tr>
            <tr>
                <td class="TitleText">Email Address</td>
                <td><input class="textInput" size="32" type="text" name="EmailAddress" value=""></td>
            </tr>
            <tr>
                <td class="TitleText">Password</td><td><input class="textInput" size="32" type="password" name="Password"></td>
            </tr>
            <tr>
                <td class="TitleText">Password (Confirm)</td>
                <td><input class="textInput" size="32" type="password" name="PasswordConfirm"></td>
            </tr>
            <tr>
                <td class="TitleText">Account Type</td>
                <td><input type="checkbox" name="Unrestricted">Unrestricted</td>
            </tr>
            <tr class="lastRow">
                <td colspan="2" align="right"><input class="button" type="submit" value="Apply"></td>
            </tr>
        </form>
    </table>
</#assign>
<#include "BaseWithNav.ftl">
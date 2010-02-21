<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#macro getacl perm>
		<#if ( perm == DENY)>
			<option value="Allow">Allow
			<option value="Deny" selected>Deny
			<option value="Undefined">Undefined
		<#elseif ( perm == PERMIT)>
			<option value="Allow" selected>Allow
			<option value="Deny">Deny
			<option value="Undefined">Undefined
		<#elseif ( perm == UNSPECIFIED)>
			<option value="Allow">Allow
			<option value="Deny">Deny
			<option value="Undefined" selected>Undefined
		</#if>
</#macro>

<#assign content>
<form action="<#if isEditing><@url name="access_control_editor" args=[objectID, "Edit", subjectType, subjectID] /><#else><@url name="access_control_editor" args=[objectID, "New"] /></#if>" method="post">

    <table cellpadding="7" cellspacing="0" width="100%" align="center">
        <tr>
            <td colspan="99" class="TopBottomBorder2">&nbsp;</td>
        </tr>
        <tr>
            <td class="TopBottomBorder2" style="width: 20px; background-color: #FFFFFF;">
                <img style="margin-top: 4px;" src="/media/img/32_Lock" alt="ACL">
            </td>
            <td class="TopBottomBorder2" style="background-color: #FFFFFF;" >
                <span class="Text_2">Access Control</span><br/>View, modify the ACLs
            </td>
        </tr>
        <#if (context.messages?? && context.messages?size > 0)>
        <tr>
            <td colspan="99">&nbsp;
            <@usermessages context />
            <#if form_errors??><@getFormErrors form_errors=form_errors /></#if>
            </td>
        </tr>
        </#if>
    </table>
    <table cellpadding="0" cellspacing="0" width="100%" align="center">
        <tr>
            <td colspan="2" >&nbsp;</td>
        </tr>
        <#-- Output the table start -->
        <tr>
            <td colspan="2">
                <table width="100%">
                    <tr class="Background1">
                        <td class="Text_2">Name</td>
                        <td class="Text_2">Operation</td>
                    </tr>
        <#-- Create the list of users and groups -->
                    <tr>
                        <td class="AlignedTop">
                            <table width="100%">
                            <#list users as user>
                                <tr>
                                    <td width="3">
                                        <input type="radio" name="Subject" value="user${user.userID?c}" <#if user.userID = userID>checked</#if><#if isEditing> disabled</#if>/>
                                    </td>
                                    <td width="3">
                                        <img src="<#if user.enabled >/media/img/16_User<#else>/media/img/16_UserDisabled</#if>" alt="user${user.userID?c}" />
                                    </td>
                                    <td>${user.userName?html} (User)<td>
                                </tr>
                            </#list>
                            <#list groups as group>
                                <tr>
                                    <td width="3">
                                        <input type="radio" name="Subject" value="group${group.groupId?c}" <#if group.groupId = groupID>checked</#if><#if isEditing> disabled</#if>/>
                                    </td>
                                    <td width="3">
                                        <img src="<#if group.enabled >/media/img/16_Group<#else>/media/img/16_GroupDisabled</#if>" alt="group${group.groupId?c}" />
                                    </td>
                                    <td>${group.groupName?html} (Group)<td>
                                </tr>
                            </#list>
                            </table>
        <#-- 2 -- Create the list of operations -->
                        <td class="AlignedTop">
                            <table>
        <#--    2.1 -- Read operation -->
                                <tr>
                                    <td>Read:</td>
                                    <td>
                                        <select name="OperationRead"><@getacl read /></select>
                                    </td>
                                </tr>
        
        <#--    3.2.2 -- Write operation -->
                                <tr>
                                    <td>Modify:</td>
                                    <td>
                                        <select name="OperationModify"><@getacl write /></select>
                                    </td>
                                </tr>
        
        <#--    3.2.3 -- Execute operation -->
                                <tr>
                                    <td>Execute:</td>
                                    <td>
                                        <select name="OperationExecute"><@getacl execute /></select>
                                    </td>
                                </tr>
        
        <#--    3.2.4 -- Delete operation -->
                                <tr>
                                    <td>Delete:</td>
                                    <td>
                                        <select name="OperationDelete"><@getacl delete /></select>
                                    </td>
                                </tr>
        
        <#--    3.2.5 -- Control operation -->
                                <tr>
                                    <td>Control:</td>
                                    <td>
                                        <select name="OperationControl"><@getacl control /></select>
                                    </td>
                                </tr>
        
        <#--    3.2.6 -- Create operation -->
                                <tr>
                                    <td>Create:</td>
                                    <td>
                                        <select name="OperationCreate"><@getacl create /></select>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr class="Background3">
            <td align="Right" colspan="7">
                <input type="hidden" name="ObjectID" value="${objectID?c}">
                <input class="button" type="Submit" value="Apply Changes" name="Apply">&nbsp;&nbsp;&nbsp;
                <input class="button" type="Submit" value="Cancel" name="Cancel">&nbsp;&nbsp;&nbsp;
                <input class="button" onClick="javascript:window.close();" type="Button" value="Close">
        
        <#if (isEditing && groupID != VALUE_UNDEFINED) >
                <input type="hidden" name="Subject" value="group"${groupID?c}">
                <input type="hidden" name="Action" value="Edit">
        <#elseif (isEditing && userID != VALUE_UNDEFINED) >
                <input type="hidden" name="Subject" value="user${userID?c}">
                <input type="hidden" name="Action" value="Edit">
        <#else>
                <input type="hidden" name="Action" value="New">
        </#if>
            </td>
        </tr>
    </table>
</form>
</#assign>
<#include "Basic.ftl">
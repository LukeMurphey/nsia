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
<form action="<#if isEditing><@url name="access_control_editor" args=["1", "Edit", subjectType, objectID] /><#else><@url name="access_control_editor" args=["1", "New"] /></#if>" method="post">
    <table cellpadding="0" cellspacing="0" width="100%" align="center">
        <tr>
            <td class="TopBottomBorder2">&nbsp;</td>
        </tr>
        <tr>
            <td class="TopBottomBorder2">
                <div style="padding-top: 6px; padding-bottom: 6px; background-color: #FFFFFF;">
                    <div style="height: 36px; position:relative; left: 10px;">
                        <img src="/media/img/32_Lock" alt="ACL">
                        <div style="position:relative; left: 40px; top: -32px;">
                            <div class="Text_2">Access Control</div>View, modify the ACLs</div>
                        </div>
                    </div>
             </td>
        </tr>
        
        <tr>
            <td>&nbsp;
            <#-- body.append(Html.renderMessages(requestDescriptor.userId)); -->
            <#if form_errors??><@getFormErrors form_errors=form_errors /></#if>
            </td>
        </tr>
        <#-- Output the table start -->
        <tr>
            <td>
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
                                    <td>${user.userName} (User)<td>
                                </tr>
                            </#list>
                            <#list groups as group>
                                <tr>
                                    <td width="3">
                                        <input type="radio" name="Subject" value="group${group.identifier?c}" <#if group.identifier = groupID>checked</#if><#if isEditing> disabled</#if>/>
                                    </td>
                                    <td width="3">
                                        <img src="<#if group.enabled >/media/img/16_Group<#else>/media/img/16_GroupDisabled</#if>" alt="group${group.identifier?c}" />
                                    </td>
                                    <td>${group.name} (Group)<td>
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
            </td>
        </tr>
    </table>
</form>
</#assign>
<#include "Basic.ftl">
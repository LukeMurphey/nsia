<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>
<form action="<@url name="access_control_editor" args=[objectID?c, "New"] />" method="get">
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
<#-- message -->
    <#macro actioncell action perm>
        <#if ( action = PERMIT )>
            <td><table><tr><td><img src="/media/img/16_up" alt="${perm?html}"></td><td>Allow</td></tr></table></td>
        <#elseif ( action = DENY )>
            <td><table><tr><td><img src="/media/img/16_down" alt="${perm?html}"></td><td>Deny</td></tr></table></td>
        <#else>
            <td>&nbsp;</td>
        </#if>
    </#macro>
    <br>
    <table width="100%">
        <tr class="Background0">
            <td width="128px" class="Text_2">Name</td>
            <td width="55px" class="Text_2">ID</td>
            <td width="55px" class="Text_2">Read</td>
            <td width="55px" class="Text_2">Modify</td>
            <td width="55px" class="Text_2">Delete</td>
            <td width="55px" class="Text_2">Execute</td>
            <td width="55px" class="Text_2">Control</td>
            <td width="55px" class="Text_2">Create</td>
            <td width="15%" colspan="2">&nbsp;</td>
        </tr>
    <#list permissions as permission>
        <#if permission.group?? >
            <#if permission.subject.enabled>
                <tr class="Background1">
                    <td>
                        <table>
                            <tr>
                                <td><img alt="Group_Active" src="/media/img/16_Group"></td>
            <#else>
                <tr class="Background1">
                    <td>
                        <table>
                            <tr>
                                <td><img alt="Group_Disabled" src="/media/img/16_GroupDisabled"></td>
            </#if>
        <#else>
            <#if permission.subject.enabled>
                <tr class="Background1">
                    <td>
                        <table>
                            <tr>
                                <td><img alt="User_Active" src="/media/img/16_User"></td>
            <#else>
                <tr class="Background1">
                    <td>
                        <table>
                            <tr>
                                <td><img alt="User_Disabled" src="/media/img/16_UserDisabled"></td>
            </#if>
        </#if>
        <#if permission.group>
                                <td>${permission.subject.name?html}</td>
                            </tr>
                        </table>
                    <td>Group ${permission.subjectID?c}</td>
        <#else>
                                <td>${permission.subject.userName?html}</td>
                            </tr>
                        </table>
                    <td>User ${permission.subjectID?c}</td>
        </#if>

        <#-- Print read -->
        <@actioncell action=permission.readPermission perm="Read" />
        
        <#-- Print modify -->
        <@actioncell action=permission.modifyPermission perm="Modify" />
        
        <#-- Print delete -->
        <@actioncell action=permission.deletePermission perm="Delete" />
        
        <#-- Print execute -->
        <@actioncell action=permission.executePermission perm="Execute" />
        
        <#-- Print control -->
        <@actioncell action=permission.controlPermission perm="Control" />
        
        <#-- Print create -->
        <@actioncell action=permission.createPermission perm="Create" />
        
        <#-- Print the edit button -->
        <#if permission.subjectType == USER>
            <td>
                <table>
                    <tr>
                        <td><img class="imagebutton" alt="edit" src="/media/img/16_Configure"></td>
                        <td><a href="<@url name="access_control_editor" args=[objectID, "Edit", "User", permission.subjectID] />">Edit</a></td>
                    </tr>
                </table>
             </td>
        <#else>
            <td>
                <table>
                    <tr>
                        <td><img class="imagebutton" alt="edit" src="/media/img/16_Configure"></td>
                        <td><a href="<@url name="access_control_editor" args=[objectID, "Edit", "User", permission.subjectID] />">Edit</a></td>
                    </tr>
                </table>
             </td>
        </#if>
        <#-- Print the delete button -->
        <#if permission.subjectType == USER>
            <td>
                <table>
                    <tr>
                        <td><img class="imagebutton" alt="delete" src="/media/img/16_Delete"></td>
                        <td><a href="<@url name="access_control_delete" args=[permission.objectID, "User", permission.subjectID] />">Delete</a></td>
                    </tr>
                </table>
            </td>
        <#else>
            <td>
                <table>
                    <tr>
                        <td><img class="imagebutton" alt="delete" src="/media/img/16_Delete"></td>
                        <td><a href="<@url name="access_control_delete" args=[permission.objectID, "User", permission.subjectID] />">Delete</a></td>
                    </tr>
                </table>
            </td>
        </#if>
        </tr>

    </#list>            
    <#if ( permissions?size == 0)>
        <tr class="Background3">
            <td colspan="99">
            <@infonote message="No Access Control List Entries Exist" />
            </td>
        </tr>
    </#if>
     	<tr class="Background3">
        	<td align="Right" colspan="10">
        		<input class="button" type="Submit" value="New Entry" name="New">&nbsp;&nbsp;&nbsp;
        		<input onClick="javascript:window.close();" class="button" type="Submit" value="Close">
        	</td>
       	</tr>
</#assign>
<#include "Basic.ftl">
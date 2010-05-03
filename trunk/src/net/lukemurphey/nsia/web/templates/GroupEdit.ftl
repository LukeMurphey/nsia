<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <div><span class="Text_1">Group Management</span>
    <br><span class="LightText"><#if group??>Edit<#else>Create New</#if> Group</span>
    <p></p>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <form action="<#if !group??><@url name="group_editor" args=["New"]/><#else><@url name="group_editor" args=["Edit", group.groupId]/></#if>" method="post">
        <#if group??><input type="hidden" name="GroupID" value="${group.groupId?c}"></#if>
        <table class="DataTable">
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Name"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Site Group Name</td>
                <td><input class="textInput" style="width: 350px;" type="text" name="Name" value="<#if request.getParameter("Name")??>${request.getParameter("Name")?html}<#elseif group??>${group.groupName?html}</#if>"></td>
            </tr>
            <tr style="vertical-align:top" class="<#if (form_errors?? && form_errors.fieldHasError("Description") )>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Description</td>
                <td><textarea style="width: 350px;" rows="8" name="Description"><#if request.getParameter("Description")??>${request.getParameter("Description")?html}<#elseif group??>${group.description?html}</#if></textarea></td>
            </tr>
            <tr class="Background3">
                <td class="alignRight" colspan="2">
                    <#if !group??><input class="button" type="Submit" name="Submit" value="Create Group"><#else><input class="button" type="Submit" name="Submit" value="Apply Changes"></#if>
                </td>
            </tr>
        </table>
    </form>
</#assign>
<#include "BaseWithNav.ftl">
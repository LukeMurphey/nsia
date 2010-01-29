<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#assign content>
    <span class="Text_1"><#if sitegroup??>Edit<#else>New</#if> Site Group</span>
    <#if sitegroup??><br><span class="LightText">${sitegroup.groupName}</span><p></#if>
    <#if (form_errors??)>Please correct the errors below and try again
    <div class="Form">
    <#list form_errors.values() as error>
        <#if error.getMessage()??><div class="Error">${error.getMessage()}</div></#if>
    </#list>
    </div>
    </#if>
    <form action="<#if !sitegroup??><@url name="sitegroup" args=["New"]/><#else><@url name="sitegroup" args=["Edit", sitegroup.groupId]/></#if>" method="post">
        <#if sitegroup??><input type="hidden" name="SiteGroupID" value="${sitegroup.groupId}"></#if>
        <table class="DataTable">
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Name"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Site Group Name</td>
                <td><input class="textInput" style="width: 350px;" type="text" name="Name" value="<#if request.getParameter("Name")??>${request.getParameter("Name")}<#elseif sitegroup??>${sitegroup.groupName}</#if>"></td>
            </tr>
            <tr style="vertical-align:top" class="<#if (form_errors?? && form_errors.fieldHasError("Description") )>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Description</td>
                <td><textarea style="width: 350px;" rows="8" name="Description"><#if request.getParameter("Description")??>${request.getParameter("Description")}<#elseif sitegroup??>${sitegroup.description}</#if></textarea></td>
            </tr>
            <tr class="Background3">
                <td class="alignRight" colspan="2">
                    <#if !sitegroup??><input class="button" type="Submit" name="Submit" value="Add Site Group"><#else><input class="button" type="Submit" name="Submit" value="Apply Changes"></#if>
                </td>
            </tr>
        </table>
    </form>
</#assign>
<#include "BaseWithNav.ftl">
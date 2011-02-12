<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <#if sitegroup??>
    <div style="display:none" id="delete_dialog" title="Delete this Site-Group?">
            <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>This Site-Group will be permanently deleted and cannot be recovered. Are you sure?</p>
    </div>
    </#if>
    
    <span class="Text_1"><#if sitegroup??>Edit<#else>New</#if> Site-group</span>
    <#if sitegroup??><br><span class="LightText">${sitegroup.groupName?html}</span><p></#if>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <form action="<#if !sitegroup??><@url name="sitegroup" args=["New"]/><#else><@url name="sitegroup" args=["Edit", sitegroup.groupId]/></#if>" method="post">
        <#if sitegroup??><input type="hidden" name="SiteGroupID" value="${sitegroup.groupId?c}"></#if>
        <table class="DataTable">
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Name"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Site-group Name</td>
                <td><input class="textInput" style="width: 350px;" type="text" name="Name" value="<#if request.getParameter("Name")??>${request.getParameter("Name")?html}<#elseif sitegroup??>${sitegroup.groupName?html}</#if>"></td>
            </tr>
            <tr style="vertical-align:top" class="<#if (form_errors?? && form_errors.fieldHasError("Description") )>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Description</td>
                <td><textarea style="width: 350px;" rows="8" name="Description"><#if request.getParameter("Description")??>${request.getParameter("Description")?html}<#elseif sitegroup??>${sitegroup.description?html}</#if></textarea></td>
            </tr>
            <tr class="Background3">
                <td class="alignRight" colspan="2">
                    <#if !sitegroup??><input class="button" type="Submit" name="Submit" value="Create Site-group"><#else><input class="button" type="Submit" name="Submit" value="Apply Changes"></#if>
                </td>
            </tr>
        </table>
    </form>
</#assign>
<#include "BaseWithNav.ftl">
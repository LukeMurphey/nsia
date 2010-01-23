<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
    <span class="Text_1">Definitions with Errors</span>
    <br />&nbsp;<br />
    <#if (errors?size > 0)>
    <table style="width: 100%" class="DataTable">
        <thead>
            <tr>
                <td>Definition Name</td>
                <td>Version</td>
                <td>ID</td>
                <td>Error</td>
                <td>First Noted</td>
                <td>Last Noted</td>
            </tr>
        </thead>
        <tbody>
        <#list errors.errorsList as error>
            <tr>
                <td>
                    <table>
                        <tr>
                            <td><img src="/media/img/16_script"</td>
                            <td><a href="<@url name="definition" args=[error.definitionID] />">${error.definitionName}</a></td>
                        </tr>
                    </table>
                </td>
                <td>${error.definitionVersion}</td>
                <td>${error.definitionID}</td>
                <td>${error.errorName}</td>
                <td>${error.dateFirstOccurred}</td>
                <td>${error.dateLastOccurred}</td>
            </tr>
        </#list>
        </tbody>
     </table>
     <#else>
        <#assign message>No errors have been observed in the current set of definitions.<p/><a href="${geturl("definitions_list")}">[View All Definitions]</a></#assign>
        <@getinfodialog title="No Errors" message=message />
     </#if>
</#assign>
<#include "BaseWithNav.ftl">
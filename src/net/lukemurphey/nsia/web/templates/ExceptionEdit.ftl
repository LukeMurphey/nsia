<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <span class="Text_1">Definition Exception</span><p>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <form action="<@url name="exception_editor" args=[ruleID] />" method="post">
        <input type="hidden" name="SiteGroupID" value="${siteGroupID?c}" />
        <#if ( definitionName?? )>
            <input type="hidden" name="DefinitionName" value="${definitionName?html}" />
        <#else>
            <input type="hidden" name="DefinitionID" value="${definitionID?c}" />
        </#if>
        <input type="hidden" name="RuleID" value="${ruleID?c}" />
        <#if ( URL?? )>
            <input type="hidden" name="URL" value="${URL?html}">
        </#if>
        
        <#if ( ReturnTo?? )>
            <input type="hidden" name="ReturnTo" value="${ReturnTo?html}" />
        </#if>
        <table>
            <tr>
                <td style="vertical-align: top;" rowspan="2">
                    <input type="radio" name="FilterType" value="Definition">
                </td>
                <td>
                    <span class="Text_3">Filter out definition (${definition.fullName?html})</span>
                </td>
            </tr>
            <tr>
                <td>Filter out findings for this specific definition only<br>&nbsp;</td>
            </tr>
            <tr>
                <td style="vertical-align: top;" rowspan="2">
                    <input type="radio" name="FilterType" value="SubCategory">
                </td>
                <td>
                    <span class="Text_3">Filter out entire sub-category (${definition.categoryName?html}.${definition.subCategoryName?html})</span>
                </td>
            </tr>
            <tr>
                <td>Filter out findings for this entire sub-category (all definitions within the sub-category)<br>&nbsp;</td>
            </tr>
            <tr>
                <td style="vertical-align: top;" rowspan="2">
                    <input type="radio" name="FilterType" value="Category">
                </td>
                <td>
                    <span class="Text_3">Filter out category (${definition.categoryName?html})</span>
                </td>
            </tr>
            <tr>
                <td>Filter out findings for this specific definition<br>&nbsp;</td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td>
                    <input class="button" type="submit" value="Add Exception" name="Add Exception">&nbsp;&nbsp;
                    <input class="button" type="submit" value="Cancel" name="Cancel">
                </td>
            </tr>
        </table>
    </form>
</#assign>
<#include "BaseWithNav.ftl">
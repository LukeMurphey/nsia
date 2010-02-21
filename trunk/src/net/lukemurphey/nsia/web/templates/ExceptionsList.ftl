<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <#if (!policies?? || policies?size == 0 )>
        <#assign message>No exceptions exist yet.</#assign>
        <@getinfodialog message=message title="No Exceptions Exist" />
    <#else>
        <span class="Text_1">Exceptions</span><br />
        <#--Revision ${definition_set_version} (Last Updated ${updated?datetime})<br>-->&nbsp;<br/>
        <form method="get" action="<@url name="exception_delete" args=[ruleID] />">
            <input type="hidden" name="SiteGroupID" value="${siteGroupID}">
            <input type="hidden" name="RuleID" value="${ruleID}">
            <table width="640" class="DataTable" summary="">
                <thead>
                    <tr>
                        <td colspan="2"><span class="Text_3">Type</span></td>
                        <td><span class="Text_3">Exception</span></td>
                        <td><span class="Text_3">Options</span></td>
                     </tr>
                </thead>
                <tbody>
            <#list policies.ruleFiltersArray as filter>
                <#if ( filter.action == INCLUDE )>
                    <#-- Don't bother showing policies that are inclusive -->
                <#else>
                    <tr>
                        <td width="6">
                            <input type="checkbox" name="ExceptionID" value="${filter.policyID}">
                        </td>
                    <#if ( filter.policyType == CATEGORY )>
                        <td>Category</td>
                        <td>${filter.definitionCategory}.*
                    <#elseif ( filter.policyType == SUBCATEGORY )>
                        <td>Sub-Category</td>
                        <td>${filter.definitionCategory}.${filter.definitionSubCategory}.*
                    <#elseif ( filter.policyType == NAME )>
                        <td>Definition</td>
                        <td>${filter.definitionCategory}.${filter.definitionSubCategory}.${filter.definitionName}
                    <#elseif ( filter.policyType == URL )>
                        <td>URL</td>
                        <td>${filter.URL}
                    </#if>
                    
                    <#if ( filter.URL?? )>for <a href="${filter.URL.toExternalForm()}" title="${filter.URL.toExternalForm()}"><@truncate_chars length=32> ${filter.URL.toExternalForm()}</@truncate_chars></a></#if>
                        </td>
                        <td>
                            <table>
                                <tr>
                                    <td class="imagebutton">
                                        <a href="<@url name="exception_delete" args=[ruleID] />?ExceptionID=${filter.policyID}"><img alt="Delete" src="/media/img/16_Delete" /></a>
                                    </td>
                                    <td>
                                        <a href="<@url name="exception_delete" args=[ruleID] />?ExceptionID=${filter.policyID}">Delete</a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                     </tr>
                </#if>
            </#list>
                    <tr class="lastRow">
                        <td colspan="99">
                            <input type="submit" class="button" name="Action" value="Delete">
                        </td>
                    </tr>
                </tbody>
            </table>
        </form>
    </#if>
</#assign>
<#include "BaseWithNav.ftl">
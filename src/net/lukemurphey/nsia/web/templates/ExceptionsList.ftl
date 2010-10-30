<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <#if (!policies?? || policies?size == 0 || policies.excludePolicyCount == 0 )>
        <#assign message>No exceptions exist yet.</#assign>
        <@getinfodialog message=message title="No Exceptions Exist" />
    <#else>
        <#include "PopupDialog.ftl">
        <#include "SelectAll.ftl">
        <span class="Text_1">Exceptions</span><br />
        Below is the list of issues that will be overlooked by the scanner<br>&nbsp;<br/>
        <form method="get" action="<@url name="exception_delete" args=[ruleID] />">
            <input type="hidden" name="SiteGroupID" value="${siteGroupID?c}">
            <input type="hidden" name="RuleID" value="${ruleID?c}">
            <table width="640" class="DataTable" summary="">
                <thead>
                    <tr>
                        <td colspan="2">
                            <div style="float:left">
                                <input type="checkbox" id="selectall">
                            </div>
                            <div>
                                <span class="Text_3">Type</span>
                            </div>

                        </td>
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
                            <input class="selectable" type="checkbox" name="ExceptionID" value="${filter.policyID?c}">
                        </td>
                    <#if ( filter.policyType == CATEGORY )>
                        <td>Category</td>
                        <td>${filter.definitionCategory?html}.*
                    <#elseif ( filter.policyType == SUBCATEGORY )>
                        <td>Sub-Category</td>
                        <td>${filter.definitionCategory?html}.${filter.definitionSubCategory?html}.*
                    <#elseif ( filter.policyType == NAME )>
                        <td>Definition</td>
                        <td><#if filter.definitionCategory??>${filter.definitionCategory?html}.</#if>${filter.definitionSubCategory?html}.${filter.definitionName?html}
                    <#elseif ( filter.policyType == URL )>
                        <td>URL</td>
                        <td>${filter.URL?html}
                    </#if>
                    
                    <#if ( filter.URL?? )>for <a href="${filter.URL.toExternalForm()?html}" title="${filter.URL.toExternalForm()?html}"><@truncate_chars length=32> ${filter.URL.toExternalForm()?html}</@truncate_chars></a></#if>
                        </td>
                        <td>
                            <table>
                                <tr>
                                    <td class="imagebutton">
                                        <a class="exceptiondelete" href="<@url name="exception_delete" args=[ruleID] />?ExceptionID=${filter.policyID?c}"><img alt="Delete" src="/media/img/16_Delete" /></a>
                                    </td>
                                    <td>
                                        <a class="exceptiondelete" href="<@url name="exception_delete" args=[ruleID] />?ExceptionID=${filter.policyID?c}">Delete</a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                     </tr>
                </#if>
            </#list>
                    <tr class="lastRow">
                        <td colspan="4">
                            <input type="submit" class="button" name="Action" value="Delete">
                            <div style="display:none"> <#-- The following button will be used solely for the jQuery script which needs to call click on a button which does not have an onClick handler (otherwise, infinite loop is caused) -->
                                <button class="button" name="Action" value="Delete">Delete</button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </form>
        <script type="text/javascript">
            $(document).ready(
                function(){
                    $('input[value=Delete][type=submit]').click(
                        function(){
                            var count = $('input.selectable:checked').length;
                            if( count == 1 ){
                                openDeleteConfirmDialog( "Are you sure you want to delete this exception? This action cannot be undone.", "Delete Exception?", function(){ $('button[value=Delete]').click(); } );
                            }
                            else if( count > 0 ){
                                openDeleteConfirmDialog( "Are you sure you want to delete these exceptions? This action cannot be undone.", "Delete Exceptions?", function(){ $('button[value=Delete]').click(); } );
                            }
                            else{
                                openDialog("No exceptions are selected. Please select an exception to delete first.", "No Exceptions Selected");
                            }
                            return false;
                        }
                    );
                    $('a.exceptiondelete').click(
                        function(){
                            openDeleteConfirmDialog( "Are you sure you want to delete this exception? This action cannot be undone.", "Delete Exception?", this.href );
                            return false;
                        }
                    );
                }
            );
        </script>
    </#if>
</#assign>
<#include "BaseWithNav.ftl">
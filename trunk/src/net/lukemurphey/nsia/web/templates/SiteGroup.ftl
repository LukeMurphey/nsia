<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#include "PopupDialog.ftl">
<#if (scanresults?? & scanresults?size = 0 && rules?size = 0) >
	<#assign message>No rules exist yet. Define a rule set to begin monitoring.<p><a href="<@url name="rule_editor" args=["New"] />?SiteGroupID=${sitegroup.groupId?c}">[Create Rule Now]</a></#assign>
    <@getinfodialog message=message title="No Rules" />
<#else>
    <#include "SelectAll.ftl">
    <form id="sitegroupform" action="${request.thisURL}" method="POST">
        <input type="hidden" name="SiteGroupID" value="${sitegroup.groupId?c}">
        <table class="DataTable" summary="HeaderEntries">
            <thead>
                <tr>
                    <td colspan="2">
                        <div style="float:left">
                            <input type="checkbox" id="selectall">
                        </div>
                        <div>
                            <span class="TitleText">Status</span>
                        </div>
                    </td>
                    <td><span class="TitleText">Description</span></td>
                    <td><span class="TitleText">Type</span></td>
                    <td><span class="TitleText">Subject</span></td>
                    <td colspan="2"><span class="Text_2">&nbsp;</span></td>
                </tr>
            </thead>
            <tbody>
            
            <#list rules as rule>
            <tr>
            <td align="center"><input class="selectable" type="checkbox" name="RuleID" value="${rule.ID?c}"></td>
            <#-- Output the status icon -->
            <#if (rule.status == STAT_GREEN)>
                <td width="40" align="center" class="StatGreen"><img src="/media/img/22_Check" alt="ok"></td>
            <#elseif ( rule.status == STAT_RED )>
                <td width="40" align="center" class="StatRed"><img src="/media/img/22_Alert" alt="alert"></td>
            <#elseif ( rule.status == STAT_BLUE )>
                <td width="40" align="center" class="StatBlue"><img src="/media/img/22_CheckBlue" alt="ok"></td>
            <#else>
                <td width="40" align="center" class="StatYellow"><img src="/media/img/22_Warning" alt="warning"></td>
            </#if>
            <#-- Output the deviation count -->
            <#if (!rule.statusDescription?? )>
                <#if ( rule.deviations == -1 )>
                    <td class="Background1">Not scanned yet&nbsp;&nbsp;</td>
                <#elseif ( rule.status == STAT_YELLOW )>
                    <td class="Background1">Connection failed&nbsp;&nbsp;</td>
                <#elseif ( rule.deviations == 1 )>
                    <td class="Background1">${rule.deviations} deviation&nbsp;&nbsp;</td>
                <#else>
                    <td class="Background1">${rule.deviations} deviations&nbsp;&nbsp;</td>
                </#if>
            <#else>
                <td class="Background1">${rule.statusDescription}&nbsp;&nbsp;</td>
            </#if>
            <#-- Output description -->
                <td class="Background1">${rule.type}&nbsp;&nbsp;</td>
                <td class="Background1">${rule.description}&nbsp;&nbsp;</td>

            <#-- Output the edit option button -->
                <td class="Background1">
                    <table>
                        <tr>
                            <td><a href="<@url name="rule_editor" args=["Edit", rule.ID] />"><img class="imagebutton" alt="configure" src="/media/img/16_Configure"></a></td>
                            <td><a href="<@url name="rule_editor" args=["Edit", rule.ID] />">Edit Rule</a></td>
                        </tr>
                    </table>
                </td>
            <#-- Output the scan result view button -->
                <td class="Background1">
                    <table>
                        <tr>
                            <td><a href="<@url name="scan_result_history" args=[rule.ID]/>"><img class="imagebutton" alt="scan results" src="/media/img/16_BarChart"></a></td>
                            <td><a href="<@url name="scan_result_history" args=[rule.ID]/>">Scan History</a></td>
                        </tr>
                    </table>
                </td>
            </tr>
            </#list>
            <tr class="lastRow">
                 <td colspan="7">
                     <input class="button" type="submit" name="Action" value="Scan">
                     <input class="button" type="submit" name="Action" value="Delete">
                     <input class="button" type="submit" name="Action" value="Baseline">
                     <div style="display:none"> <#-- The following button will be used solely for the jQuery script which needs to call click on a button which does not have an onClick handler (otherwise, infinite loop is caused) -->
                        <button class="button" name="Action" value="Delete">Delete</button>
                     </div>
                 </td>
             </tr>
           </tbody>
        </table>
        
        <table>
            <tr>
                <td width="4"></td>
                <td><img src="/media/img/16_Add"></td>
                <td><a href="<@url name="rule_editor" args=["New"] />?SiteGroupID=${sitegroup.groupId?c}">[Add a new rule]</a></td>
            </tr>
        </table>
        
        <script type="text/javascript">
            $(document).ready(
                function(){
                    $('input[value=Delete][type=submit]').click(
                        function(){
                            
                            var count = $('input.selectable:checked').length;
                            if( count == 1 ){
                                openDeleteConfirmDialog( "Are you sure you want to delete this rule? This action cannot be undone.", "Delete Rule?", function(){ $('button[value=Delete]').click(); } );
                            }
                            else if( count > 0 ){
                                openDeleteConfirmDialog( "Are you sure you want to delete these rules? This action cannot be undone.", "Delete Rules?", function(){ $('button[value=Delete]').click(); } );
                            }
                            else{
                                openDialog("No rules are selected. Please select a rule to delete first.", "No Rules Selected");
                            }
                            return false;
                        }
                     );
                    $('input[value=Baseline][type=submit]').click(
                        function(){
                            var count = $('input.selectable:checked').length;
                            if( count == 0 ){
                                openDialog("No rules are selected. Please select a rule to baseline first.", "No Rules Selected");
                                return false;
                            }
                            showHourglass('Baselining...');
                            pauseCountdown();
                        }
                     );
                    $('input[value=Scan2][type=submit]').click(
                        function(){
                            
                            var count = $('input.selectable:checked').length;
                            if( count == 0 ){
                                openDialog("No rules are selected. Please select a rule to scan first.", "No Rules Selected");
                                return false;
                            }
                            showHourglass('Scanning...');
                            pauseCountdown();
                        }
                     );
                }
            );
        </script>
</#if>
</#assign>
<#include "BaseWithNav.ftl">
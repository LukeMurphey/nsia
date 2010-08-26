<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#if (scanresults?? & scanresults?size = 0 && rules?size = 0) >
	<#assign message>No rules exist yet. Define a rule set to begin monitoring.<p><a href="<@url name="rule_editor" args=["New"] />?SiteGroupID=${sitegroup.groupId?c}">[Create Rule Now]</a></#assign>
    <@getinfodialog message=message title="No Rules" />
<#else>
    <form action="${request.thisURL}">
        <input type="hidden" name="SiteGroupID" value="${sitegroup.groupId?c}">
        <table class="DataTable" summary="HeaderEntries">
            <thead>
                <tr>
                    <td colspan="2"><span class="TitleText">Status</span></td>
                    <td><span class="TitleText">Description</span></td>
                    <td><span class="TitleText">Type</span></td>
                    <td><span class="TitleText">Subject</span></td>
                    <td colspan="2"><span class="Text_2">&nbsp;</span></td>
                </tr>
            </thead>
            <tbody>
            
            <#list rules as rule>
            <tr>
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
            <td align="center"><input type="checkbox" name="RuleID" value="${rule.ID?c}"></td>
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
                            <td><a href="<@url name="rule_editor" args=["Edit", rule.ID] />">Details</a></td>
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
                     <input onClick="showHourglass('Scanning...'); pauseCountdown();" class="button" type="submit" name="Action" value="Scan">
                     <input onClick="pauseCountdown();" class="button" type="submit" name="Action" value="Delete">
                     <input onClick="showHourglass('Baselining...'); pauseCountdown();" class="button" type="submit" name="Action" value="Baseline">
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
</#if>
</#assign>
<#include "BaseWithNav.ftl">
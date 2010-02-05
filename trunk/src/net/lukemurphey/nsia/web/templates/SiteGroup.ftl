<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#if (scanresults?? & scanresults?size = 0 && rules?size = 0) >
    <@getinfodialog message="No rules exist yet. Define a rule set to begin monitoring.<p><a href=\"\">[Create Rule Now]</a>" title="No Rules" />
<#else>
    <form action="${request.thisURL}">
        <input type="hidden" name="SiteGroupID" value="${sitegroup.groupId}">
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
            <td align="center"><input type="checkbox" name="RuleID" value="${rule.ID}"></td>
            <#-- Output the deviation count -->
            <#if (!rule.status_description?? )>
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
                <td class="Background1">${rule.status_description}"&nbsp;&nbsp;</td>";
            </#if>
                <#-- Output description -->
                <td class="Background1">${rule.type}&nbsp;&nbsp;</td>
                <td class="Background1">${rule.description}&nbsp;&nbsp;</td>
            
            <#-- Output the delete option button
            /*output += "<td class="Background1"><table><tr><td><a href="SiteGroup?Action=DeleteRule&RuleID=${rule.ID}&SiteGroupID=" + siteGroupID +
            "><img alt="delete" src="/16_Delete"></a></td><td><a href="SiteGroup?Action=DeleteRule&RuleID=${rule.ID}&SiteGroupID=" + siteGroupID +
            ">Delete</a></td></tr></table></td></td>";*/  -->
            
            <#-- Output the edit option button -->
                <td class="Background1">
                    <table>
                        <tr>
                            <td><a href="ScanRule?Action=Edit&RuleID=${rule.ID}"><img class="imagebutton" alt="configure" src="/16_Configure"></a></td>
                            <td><a href="ScanRule?RuleID=${rule.ID}">Details</a></td>
                        </tr>
                    </table>
                </td>
            <#-- Output the scan result view button -->
                <td class="Background1">
                    <table>
                        <tr>
                            <td><a href="ScanResult?RuleID=${rule.ID}"><img class="imagebutton" alt="scan results" src="/16_BarChart"></a></td>
                            <td><a href="ScanResult?RuleID=${rule.ID}">Scan History</a></td>
                        </tr>
                    </table>
                </td>
            <#-- Output the scan button
            /*output += "<td class="Background1"><table><tr><td><a href="SiteGroup?Action=Scan&RuleID=${rule.ID}><img class="imagebutton" alt="scan" src="/16_Play"></a></td><td><a href="SiteGroup?Action=Scan&RuleID=" + ruleId +    
            ">Scan</a></td></tr></table></td>"; -->
            </tr>
            </#list>
            <tr class="lastRow">
                 <td colspan="99">
                     <input onClick="showHourglass('Scanning...'); pauseCountdown();" class="button" type="submit" name="Action" value="Scan">
                     <input onClick="pauseCountdown();" class="button" type="submit" name="Action" value="Delete">
                     <input onClick="pauseCountdown();" class="button" type="submit" name="Action" value="Baseline">
                 </td>
             </tr>
           </tbody>
        </table>
</#if>
</#assign>
<#include "BaseWithNav.ftl">
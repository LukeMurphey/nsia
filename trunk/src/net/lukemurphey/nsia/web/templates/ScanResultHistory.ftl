<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#include "Math.ftl">
<#assign content>
    <#if (!results?? | results?size == 0) >
        <@getinfodialog message="No scan results exist for the given rule yet." title="No Scan Results" />
    <#else>
        <span class="Text_1">Scan Results</span>
        <#if results??><br><span class="LightText">Viewing ${min(count, results?size)} results</span><p></#if>
        
        <table width="660px" class="DataTable">
            <tbody>
                <tr>
                    <td class="BackgroundLoading1" height="150"><img src="/graph/RuleScanHistory?RuleID=${ruleID?c}&t=${current_time_millis?c}&S=${firstScanResultID?c}" alt="Rule History"></td>
                </tr>
            </tbody>
        </table>
        <table width="660px" class="DataTable" summary="">
            <thead>
                <tr>
                    <td width="48px">Status</td>
                    <td>Result</td>
                    <td colspan="3">Time Scanned</td>
                </tr>
            </thead>
            <tbody>
            <#list results as result> 
                <tr>
                <#-- 1 -- Output the status icon -->
                <#if (result.deviations == 0 && result.resultCode == SCAN_COMPLETED) >
                    <td align="center" class="StatGreen"><img src="/media/img/22_Check" alt="ok"></td>
                    <td class="Background1">0 deviations&nbsp;&nbsp;</td>
                <#elseif (result.deviations > 0 )>
                    <td align="center" class="StatRed"><img src="/media/img/22_Alert" alt="alert"></td>
                    <td class="Background1">${result.deviations} deviations&nbsp;&nbsp;</td>
                <#else>
                    <td align="center" class="StatYellow"><img src="/media/img/22_Warning" alt="warning"></td>
                    <td class="Background1">${result.resultCode.description?html}&nbsp;&nbsp;</td>
                </#if>
                <#-- 2 -- Output the time that the resource was scanned  -->
                    <td class="Background1">${result.scanTime?datetime}&nbsp;&nbsp;</td>
                <#-- 3 -- Output the edit option button  -->
                    <td class="Background1">
                        <table>
                            <tr>
                                <td><a href="<@url name="scan_result" args=[result.scanResultID] />"><img class="imagebutton" alt="configure" src="/media/img/16_magnifier"></a></td>
                                <td><a href="<@url name="scan_result" args=[result.scanResultID] />">View Details</a></td>
                            </tr>
                        </table>
                    </td>
                 </tr>
            </#list>
            </tbody>
        </table>
            
        <br />
        <form action="<@url name="scan_result_history" args=[ruleID] />">
           <input type="hidden" name="RuleID" value="${ruleID?c}">
           <#if ( maxEntry > -1 && firstScanResultID == maxEntry )>
           <input disabled="true" class="buttonDisabled" type="submit" name="Action" value="Previous">
           <#else>
           <input class="button" type="submit" name="Action" value="Previous">
           </#if>
           
           <#if ( minEntry > -1 && lastScanResultID == minEntry )>
           <input disabled="true" class="buttonDisabled" type="submit" name="Action" value="Next">
           <#else>
           <input class="button" type="submit" name="Action" value="Next">
           </#if>
           <input type="hidden" name="S" value="${lastScanResultID?c}">
           <input type="hidden" name="E" value="${firstScanResultID?c}">
        </form>
    </#if>
</#assign>
<#include "BaseWithNav.ftl">
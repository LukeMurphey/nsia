<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <div><span class="Text_1">Service Scan Rule</span>
    <br><span class="LightText">Monitors a server to identify when new ports are opened or when existing ports are closed</span><p>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <script src="/media/misc/codepress/codepress.js" type="text/javascript"></script>
	<#assign jquerytools><script type="text/javascript" language="javascript" src="/media/js/jquery.tools.min.js"></script></#assign>
    <#assign extrafooter=[jquerytools] />
    <script type="text/javascript">
        function submitEditorForm(editorform){
            document.editorform.PortsExpectedOpen2.value = cp2.getCode();
            document.editorform.PortsToScan2.value = cp1.getCode();
            document.editorform.submit();
            return true;
        }
        
		$(document).ready( function(){
			$("td[title]").tooltip({
		
				// place tooltip on the right edge
				position: "center right",
		
				// a little tweaking of the position
				offset: [-2, 10],
		
				// use the built-in fadeIn/fadeOut effect
				effect: "fade",
		
				// custom opacity setting
				opacity: 0.8
		
			}).dynamic({ bottom: { direction: 'down', bounce: true } });
		});
    </script>
    
    <form name="editorform" id="editorform" onSubmit="return submitEditorForm(this.form)" action="<#if rule??><@url name="rule_editor" args=["Edit", rule.ruleId]/><#else><@url name="rule_editor" args=["New"]/></#if>" method="post">
        <table class="DataTable">
            <#-- 1 -- Output scan frequency -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanFrequencyValue"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Scan Frequency</td>
                <td title="Define how frequently the scan should execute"><input class="textInput" type="text" name="ScanFrequencyValue" value="${scanFrequencyValue?c}" />&nbsp;&nbsp;
                    <select name="ScanFrequencyUnits">
                        <option value="86400"<#if scanFrequencyUnits == 84600> selected</#if>>Days</option>
                        <option value="3600"<#if scanFrequencyUnits == 3600> selected</#if>>Hours</option>
                        <option value="60"<#if scanFrequencyUnits == 60> selected</#if>>Minutes</option>
                        <option value="1"<#if scanFrequencyUnits == 1> selected</#if>>Seconds</option>
                    </select>
                </td>
            </tr>
            <#-- 2 -- Output server address -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Server"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Server Address</td>
                <td title="Define the address of the server to scan; either the domain name (e.g. threatfactor.com) or the IP address (e.g. 10.2.3.4)"><input class="textInput" size="40" type="text" name="Server" value="<#if request.getParameter("Server")??>${request.getParameter("Server")?html}<#elseif rule??>${rule.serverAddress?html}</#if>"></td>
            </tr>
            <#-- 3 -- Output ports to scan -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("PortsToScan"))>ValidationFailed<#else>Background1</#if>">
                <td style="vertical-align: top;" class="Text_3">Ports to Scan</td>
                <td title="Define the ports that will be scanned with each port range on a separate line (e.g. TCP\80, UDP\500-514)."><textarea id="cp1" class="codepress ports" wrap="virtual" rows="6" cols="48" name="PortsToScan"><#if request.getParameter("PortsToScan")??>${request.getParameter("PortsToScan")?html}<#elseif rule??><#list rule.portsToScan as port>${port?html}<@endline /></#list></#if></textarea>
                    <input type="hidden" name="PortsToScan2" value="<#if request.getParameter("PortsToScan2")??>${request.getParameter("PortsToScan2")?html}<#elseif rule??><#list rule.portsToScan as port>${port?html}<@endline /></#list></#if>"></td>
            </tr>
            <#-- 4 -- Output ports expected open -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("PortsExpectedOpen"))>ValidationFailed<#else>Background1</#if>">
                <td style="vertical-align: top;" class="Text_3">Ports Expected Open</td>
                <td title="Define the ports that are expected to open with each port range on a separate line (e.g. TCP\80, UDP\500-514)."><textarea id="cp2" class="codepress ports" wrap="virtual" rows="6" cols="48" name="PortsExpectedOpen"><#if request.getParameter("PortsExpectedOpen")??>${request.getParameter("PortsExpectedOpen")?html}<#elseif rule??><#list rule.portsExpectedOpen as port>${port?html}<@endline /></#list></#if></textarea>
                    <input type="hidden" name="PortsExpectedOpen2" value="<#if request.getParameter("PortsExpectedOpen2")??>${request.getParameter("PortsExpectedOpen2")?html}<#elseif rule??><#list rule.portsExpectedOpen as port>${port?html}<@endline /></#list></#if>"></td>
            </tr>
            <tr class="lastRow">
                <td class="alignRight" colspan="2">
                <#if !rule?? >
                    <input class="button" type="submit" value="Create Rule" name="Action">&nbsp;&nbsp;
                    <input type="hidden" name="SiteGroupID" value="${siteGroupID?c}">
                    <input type="hidden" name="RuleType" value="${request.getParameter("RuleType")?html}">
                <#else>
                    <input class="button" type="submit" value="Apply Changes" name="Action">&nbsp;&nbsp;
                    <input type="hidden" name="RuleID" value="${rule.ruleId?c}">
                </#if>
                    <input class="button" type="submit" value="Cancel" name="Action">
                </td>
            </tr>
        </table>
     </form>
</#assign>
<#include "BaseWithNav.ftl">
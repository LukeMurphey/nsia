<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <div><span class="Text_1">HTTP Content Auto-Discovery Rule</span>
    <br><span class="LightText">Automatically crawls the website in order to identify malicious content</span>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <script src="/media/misc/codepress/codepress.js" type="text/javascript"></script>
    <script type="text/javascript">
        function submitEditorForm(editorform){
            document.editorform.StartAddresses2.value = cp1.getCode();
            document.editorform.submit();
            return true;
        }
    </script>
    <form name="editorform" id="editorform" onSubmit="return submitEditorForm(this.form)" action="<#if rule??><@url name="rule_editor" args=["Edit", rule.ruleId]/><#else><@url name="rule_editor" args=["New"]/></#if>" method="post">
        <input type="hidden" name="StartAddresses2" value="<#if request.getParameter("StartAddresses")??>${request.getParameter("StartAddresses")?html}<#elseif rule??><#list rule.seedUrls as url>${url}<@endline /></#list></#if>">
        <table class="DataTable">
            <#-- 1 -- Output scan frequency -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanFrequencyValue"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Scan Frequency</td>
                <td><input class="textInput" type="text" name="ScanFrequencyValue" value="${scanFrequencyValue?c}" />&nbsp;&nbsp;
                    <select name="ScanFrequencyUnits">
                        <option value="86400"<#if scanFrequencyUnits == 84600> selected</#if>>Days</option>
                        <option value="3600"<#if scanFrequencyUnits == 3600> selected</#if>>Hours</option>
                        <option value="60"<#if scanFrequencyUnits == 60> selected</#if>>Minutes</option>
                        <option value="1"<#if scanFrequencyUnits == 1> selected</#if>>Seconds</option>
                    </select>
                </td>
            </tr>
            <#-- 2 -- Output the start addresses -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("StartAddresses"))>ValidationFailed<#else>Background1</#if>">
                <td style="vertical-align: top;"><div style="margin-top: 5px;" class="TitleText">Addresses to Scan:</div></td>
                <td><textarea id="cp1" class="codepress urls autocomplete-off" wrap="virtual" rows="11" cols="48" name="StartAddresses"><#if request.getParameter("StartAddresses")??>${request.getParameter("StartAddresses")?html}<#elseif rule??><#list rule.seedUrls as url>${url?trim?html}<@endline /></#list></#if></textarea></td>
            </tr>
            <#-- 3 -- Output the domain limiter -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Domain"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Domain</td>
                <td><input class="textInput" size="40" type="text" name="Domain" value="<#if request.getParameter("Domain")??>${request.getParameter("Domain")?html}<#elseif rule??>${rule.domainRestriction?html}</#if>"></td>
            </tr>
            <#-- 4 -- Output the recursion depth -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("RecursionDepth"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Levels to Recurse</td>
                <td><input class="textInput" size="40" type="text" name="RecursionDepth" value="<#if request.getParameter("RecursionDepth")??>${request.getParameter("RecursionDepth")?html}<#elseif rule??>${rule.recursionDepth?c}</#if>"></td>
            </tr>
            <#-- 5 -- Output the scan limit -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanLimit"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Maximum Number of Resource to Scan</td>
                <td><input class="textInput" size="40" type="text" name="ScanLimit" value="<#if request.getParameter("ScanLimit")??>${request.getParameter("ScanLimit")?html}<#elseif rule??>${rule.scanCountLimit?c}</#if>"></td>
            </tr>
            <tr class="lastRow">
                <td class="alignRight" colspan="99">
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
</#assign>
<#include "BaseWithNav.ftl">
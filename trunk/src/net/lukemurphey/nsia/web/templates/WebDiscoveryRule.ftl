<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <div><span class="Text_1">HTTP Content Auto-Discovery Rule</span>
    <br><span class="LightText">Automatically crawls the website in order to identify malicious content</span>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <script src="/media/codepress/codepress.js" type="text/javascript"></script>
    
    <script type="text/javascript">
        function submitEditorForm(editorform){
            document.editorform.StartAddresses2.value = cp1.getCode();
            document.editorform.submit();
            return true;
        }
    </script>
    <form name="editorform" id="editorform" onSubmit="return submitEditorForm(this.form)" action="HttpDiscoveryRule" method="post">
        <input type="hidden" name="StartAddresses2" value="${startAddresses}">
        <table class="DataTable">
            <#-- 3 -- Output scan frequency -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanFrequencyValue"))>ValidationFailed<#else>Background1</#if>">
                <td class="Text_3">Scan Frequency</td>
                <td><input class="textInput" type="text" name="ScanFrequencyValue" value="<#if request.getParameter("ScanFrequencyUnits")??>${request.getParameter("ScanFrequencyUnits")}<#elseif rule??>${rule.scanFrequency}</#if>" />&nbsp;&nbsp;
                    <select name="ScanFrequencyUnits">
                        <option value="86400"<#if scanFrequencyUnits == 84600> selected</#if>>Days</option>
                        <option value="3600"<#if scanFrequencyUnits == 3600> selected</#if>>Hours</option>
                        <option value="60"<#if scanFrequencyUnits == 60> selected</#if>>Minutes</option>
                        <option value="1"<#if scanFrequencyUnits == 1> selected</#if>>Seconds</option>
                    </select>
                </td>
            </tr>
            <#-- 4 -- Output the start addresses -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("StartAddresses"))>ValidationFailed<#else>Background1</#if>">
                <td style="vertical-align: top;"><div style="margin-top: 5px;" class="TitleText">Addresses to Scan:</div></td>
                <td><textarea id="cp1" class="codepress urls autocomplete-off" wrap="virtual" rows="11" cols="48" name="StartAddresses"><#if request.getParameter("StartAddresses")??>${request.getParameter("StartAddresses")}<#elseif rule??><#list rule.seedUrls as url>${url}&nbsp;</#list></#if></textarea></td>
            </tr>
            <#-- 5 -- Output the domain limiter -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Domain"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Domain</td>
                <td><input class="textInput" size="40" type="text" name="Domain" value="<#if request.getParameter("Domain")??>${request.getParameter("Domain")}<#elseif rule??>${rule.domainRestriction}</#if>"></td>
            </tr>
            <#-- 6 -- Output the recursion depth -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("RecursionDepth"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Levels to Recurse</td>
                <td><input class="textInput" size="40" type="text" name="RecursionDepth" value="<#if request.getParameter("RecursionDepth")??>${request.getParameter("RecursionDepth")}<#elseif rule??>${rule.recursionDepth}</#if>"></td>
            </tr>
            <#-- 7 -- Output the scan limit -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanLimit"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Maximum Number of Resource to Scan</td>
                <td><input class="textInput" size="40" type="text" name="ScanLimit" value="<#if request.getParameter("ScanLimit")??>${request.getParameter("ScanLimit")}<#elseif rule??>${rule.scanCountLimit}</#if>"></td>
            </tr>
            <tr class="lastRow">
                <td class="alignRight" colspan="99">
                <#if scanrule >
                    <input class="button" type="submit" value="Add" name="Action">&nbsp;&nbsp;
                    <input type="hidden" name="SiteGroupID" value="${siteGroupID}">
                <#else>
                    <input class="button" type="submit" value="Edit" name="Action">&nbsp;&nbsp;
                    <input type="hidden" name="RuleID" value="${rule.ruleId}">
                </#if>
                    <input class="button" type="submit" value="Cancel" name="Action">
                </td>
            </tr>
         </table>
</#assign>
<#include "BaseWithNav.ftl">
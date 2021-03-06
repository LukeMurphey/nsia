<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>
    <#macro summaryrow name value>
        <tr class="Background1"><td class="Text_3">${name?html}</td><td class="Text_3">${value?html}</td></tr>
    </#macro>
    <script>
        function toggle(obj) {
           var el = document.getElementById(obj);
           var e3 = document.getElementById(obj+'off');
           var e2 = document.getElementById(obj+'on');
           if ( el.style.display != 'none' ) {
               el.style.display = 'none';
               e2.style.display = 'none';
               e3.style.display = '';
           }
           else {
               el.style.display = '';
               e2.style.display = '';
               e3.style.display = 'none'
           }
        }
        
        function escapeHTML( content ) {                                        
                return(                                                                 
                    content.replace(/&/g,'&amp;').                                         
                        replace(/>/g,'&gt;').                                           
                        replace(/</g,'&lt;').                                           
                        replace(/"/g,'&quot;')                                         
                );                                                                      
            };
        
        <#-- A dialog to confirm the deletion of an object -->
        function openURLDialog(url, content_type){
            
            <#-- Show the dialog -->
            var $dialog = $('<div></div>')
                .html( '<div style="padding: 16px;">' +
'The details of the scan result for the finding is below. Note that the URL should only be opened if you know it is safe or you have taken necessary precautions.' +
'<table style="margin-left: 16px;margin-top:16px;">' +
'    <tr><td><strong>URL</strong></td><td><a href="' + encodeURI(url) + '" target="url_viewer" style="color: blue;">' + escapeHTML(url) + '</a></td></tr>' +
'    <tr><td><strong>Content-Type</strong>&nbsp;</td><td>' + escapeHTML(content_type) + '</td></tr>' +
'</table>' +
'</div>' )
                .dialog({
                    autoOpen: true,
                    title: "Scan Result URL",
                    modal: true,
                    width: 600,
                    height: 300,
                    buttons: {
                        Ok: function() {
                            $( this ).dialog( "close" );
                            return false;
                        }
                    }
                });
     }
        
     </script>
     <#-- Print out the result table -->
     <table cellpadding="2">
        <tr class="Background0">
            <td colspan="2" class="Text_3">Scan Parameters</td>
        </tr>
        <@summaryrow "Deviations" scanResult.deviations />
        <#assign scanned>
            ${scanResult.accepts + scanResult.deviations - scanResult.incompletes}
        </#assign>
        <@summaryrow "Resources Scanned" scanned />
        <#if scanResult.specimenDescription??><@summaryrow "Domain" scanResult.specimenDescription /></#if>
        <@summaryrow "Date Scanned" scanResult.scanTime?datetime />
    </table>
    <#-- Render definitions match table -->
    <p>
    <table width="700px" cellpadding="2">
        <#if ( definitionMatches?size == 0) >
            <tr class="Background0">
                <td height="8px" class="Text_3">Definitions Matched</td>
            </tr>
            <tr class="Background1"><td><@infonote message="No definitions matches observed" /></td></tr>
        <#else>
            <tr class="Background0">
                <td height="8px" colspan="3" class="Text_3">Definitions Matched</td>
            <#if ( definitionMatches?size >= 5) >
                <td width="400px" rowspan="99" style="vertical-align:top" class="BackgroundLoading1"><img alt="ContentTypes" src="/graph/SeverityResults?ResultID=${scanResult.scanResultID?c}&H=${(25 + (definitionMatches?size * 23))?c}"></td></tr>
            <#else>
                <td width="400px" rowspan="99" style="vertical-align:top" class="BackgroundLoading1"><img alt="ContentTypes" src="/graph/SeverityResults?ResultID=${scanResult.scanResultID?c}"></td></tr>
            </#if>
         
            <#list definitionMatches as match>
                <#assign returnURL><@url name="scan_result" args=[scanResult.scanResultID] /></#assign>
                <#if ( siteGroup?? ) >
                    <tr class="Background1">
                        <td height="8px" class="Text_3"><a href="<@url name="scan_result" args=[scanResult.scanResultID] />?RuleFilter=${match.name?html}">${match.name?html}</a></td>
                <#else>
                    <tr class="Background1">
                        <td height="8px" class="Text_3"><a href="SiteScan?RuleFilter=${match.name?html}&ResultID=${scanResult.scanResultID?c}">${match.name?html}</a></td>
                </#if>
                
                <#if (siteGroup.groupId >= 0) >
                        <td class="Text_3" width="8"><a href="<@url name="exception_editor" args=[rule.ruleId]/>?DefinitionName=${match.name?html}&ReturnTo=${returnURL}"><img class="imagebutton" alt="Filter" src="/media/img/16_Filter"></a></td>
                </#if>
                        <td height="8px" class="Text_3">${match.value?html}</td>
                   </tr>
            </#list>
            <#if (definitionMatches?size < 6)>
                <tr class="Background1"><td height="${((6-definitionMatches?size)*22)?c}" colspan="3"></td></tr>
            </#if>
        </#if>
    </table>
    <#-- Content Type table -->
    <p>
    <table width="700px" cellpadding="2">
        <tr class="Background0">
            <td height="8px" colspan="2" class="Text_3">Content Types</td>
            <td style="vertical-align:top" width="400px" rowspan="99" class="BackgroundLoading1">
            <#assign contentTypes=scanResult.discoveredContentTypes />
            <#if ( contentTypes?size >= 5 ) > 
                <img alt="ContentTypes" src="/graph/ContentTypeResults?ResultID=${scanResult.scanResultID?c}&H=${(25 + (contentTypes?size * 20))?c}"></td></tr>
            <#else>
                <img alt="ContentTypes" src="/graph/ContentTypeResults?ResultID=${scanResult.scanResultID?c}"></td>
            </#if>
        </tr>
        <#if contentTypesCount?size == 0 >
        <tr class="Background1"><td colspan="99"><@infonote message="No resources scanned" /></td></tr>
        <#else>
            <#list contentTypesCount as contentType >
            <#assign name><#if contentType.name??>${contentType.name?html}<#else>[Unknown]</#if></#assign>
            <#if siteGroup??>
            <tr class="Background1">
                <td height="8px" class="Text_3"><a href="<@url name="scan_result" args=[scanResult.scanResultID] />?ContentTypeFilter=${name?html}">
            <#else>
            <tr class="Background1">
                <td height="8px" class="Text_3"><a href="/SiteScan?ContentTypeFilter=${c}&ResultID=${scanResult.scanResultID?c}">
            </#if>
            ${name?html}</a></td><td height="8px" class="Text_3">${contentType.value?c}</td></tr>
            </#list>
        </#if>
        
        <#if (contentTypesCount?size < 5) >
            <tr class="Background1"><td height="${((5-contentTypesCount?size)*30)?c}px" colspan="2"></td></tr>
        </#if>
        </table>
    <#-- Render definitions matching list -->
    <table width="700" cellpadding="2">
        <#if ( findings?size == 0)>
            <#if scanRuleFilter?? || contentTypeFilter??>
        <tr>
            <td colspan="99"><#assign message>No resources match the provided filter.<p/><a href="${request.thisURL?html}">[Clear Filter]</a></#assign>
            <@getinfodialog title="No Findings" message=message /><td>
        </tr>
            <#else>
        <tr>
            <td colspan="99"><@getinfodialog title="No Findings" message="No resources were scanned during the scan phase." /><td>
        </tr>
            </#if>
        <#else>
        <br/>
            <#if scanRuleFilter?? && contentTypeFilter??>
        <tr>
            <td colspan="4">
                <#assign message>Displaying findings that matched the <u>${scanRuleFilter?html}</u> definition and the <u>${contentTypeFilter?html}</u> content-type.<br><a href="${request.thisURL?html}">[Clear Filter]</a></#assign>
                <@getinfodialog title="Filters Applied" message=message />
            </td>
        </tr>
            <#elseif scanRuleFilter??>
        <tr>
            <td colspan="4">
                <#assign message>Displaying findings that matched the <u>${scanRuleFilter?html}</u> definition.<br><a href="${request.thisURL?html}">[Clear Filter]</a></#assign>
                <@getinfodialog title="Filters Applied" message=message />
            </td>
        </tr>
            <#elseif contentTypeFilter??>
        <tr>
            <td colspan="4">
                <#assign message>Displaying findings that matched the <u>${contentTypeFilter?html}</u> content-type.<br><a href="${request.thisURL?html}">[Clear Filter]</a></#assign>
                <@getinfodialog title="Filters Applied" message=message />
            </td>
        </tr>
            </#if>
        <tr class="Background0">
            <td colspan="4" class="Text_3">Scan Findings</td>
        </tr>
        
            <#list findings as finding>
            <#assign severity=finding.maxSeverity />
            <#if ( finding.resultCode == SCAN_COMPLETED && finding.deviations == 0 )>
         <tr class="Background1">
            <td width="22" style="vertical-align: top;" class="StatGreenSmall"><img src="/media/img/22_Check" alt="OK"></td>
            <#elseif ( finding.resultCode == SCAN_COMPLETED && finding.deviations > 0 )>
                <#if ( severity == HIGH )>
         <tr class="Background1">
            <td width="22" style="vertical-align: top;" class="StatRedSmall"><img src="/media/img/22_Alert" alt="Alert"></td>
                <#elseif (severity == MEDIUM)>
          <tr class="Background1">
            <td width="22" style="vertical-align: top;" class="StatYellowSmall"><img src="/media/img/22_Warning" alt="Warning"></td>
                <#else>
          <tr class="Background1">
            <td width="22" style="vertical-align: top;" class="StatBlueSmall"><img src="/media/img/22_Information" alt="Info"></td>
                </#if>
            <#else>
          <tr class="Background1">
            <td width="22" style="vertical-align: top;" class="StatYellowSmall"><img src="/media/img/22_Warning" alt="Warning"></td>
            </#if>
            <td title="${finding.url?html}" style="vertical-align:middle;">
                <img style="display: none;" id="finding${finding_index?c}on" onclick="toggle('finding${finding_index?c}')" src="/media/img/9_TreeNodeOpen" alt="Node">
                <img id="finding${finding_index?c}off" onclick="toggle('finding${finding_index}')" src="/media/img/9_TreeNodeClosed" alt="Node">&nbsp;
                <a onclick="openURLDialog('${finding.url?html}', '<#if finding.contentType??>${finding.contentType?html}<#else>unknown</#if>');return false;"><span class="Text_3"><@truncate_chars length=64>${finding.url?html}</@truncate_chars></span></a>
                <#assign matches = finding.definitionMatches />
                    <#if (matches?size > 0 )>
                <div style="display: none;" id="finding${finding_index?c}">
                        <#list matches as match >
                    <p>&nbsp;&nbsp;&nbsp;<strong>${match.definitionName?html}</strong>
                            <#if ( siteGroup.groupId > -1 )>
                            <#assign returnURL><@url name="scan_result" args=[finding.parentScanResultID] /></#assign>
                            <a href="<@url name="exception_editor" args=[rule.ruleId]/>?DefinitionName=${match.definitionName?html}&ReturnTo=${returnURL?html}&URL=${finding.url?html}">&nbsp;[Create Exception...]</a>
                            </#if>
                        <br>&nbsp;&nbsp;&nbsp;${match.message?html}
                        </#list>
                    </div>
                  <#else>
                    <div style="display: none;" id="finding${finding_index?c}"><p>&nbsp;&nbsp;&nbsp;No Definitions Matched</div>
                  </#if>
              </td>
                <#if ( finding.resultCode == SCAN_COMPLETED && finding.deviations == 0)>
                <td width="180" style="vertical-align: top;">No Issues Found&nbsp;&nbsp;&nbsp;</td>
                <td class="StatGreenSmall">&nbsp;</td>
                <#elseif ( finding.resultCode == SCAN_COMPLETED && finding.deviations > 0 )>
                    <#if ( severity == HIGH )>
                <td width="180" style="vertical-align: top;">Definitions Matched&nbsp;&nbsp;&nbsp;</td>
                <td class="StatRedSmall">&nbsp;</td>
                    <#elseif (severity == MEDIUM)>
                <td width="180" style="vertical-align: top;">Definitions Matched&nbsp;&nbsp;&nbsp;</td>
                <td class="StatYellowSmall">&nbsp;</td>
                    <#else>
                <td width="180" style="vertical-align: top;">Definitions Matched&nbsp;&nbsp;&nbsp;</td>
                <td class="StatBlueSmall">&nbsp;</td>
                    </#if>
                <#else>
                <td width="180" style="vertical-align: top;">Scan issues noted&nbsp;&nbsp;&nbsp;</td>
                <td class="StatYellowSmall">&nbsp;</td>
                </#if>
            </tr>
            </#list>
        </#if>
        </table>
    
    <#-- Render pagination -->
    <br>
    <form action="<@url name="scan_result" args=[scanResult.scanResultID] />">
    
        <#if ( maxMinCount?? && firstScanResultID == maxMinCount.min) >
            <input disabled="true" class="buttonDisabled" type="submit" name="Action" value="Previous">
        <#else>
            <input class="button" type="submit" name="Action" value="Previous">
        </#if>
        
        <#if ( maxMinCount?? && lastScanResultID == maxMinCount.max)>
            <input disabled="true" class="buttonDisabled" type="submit" name="Action" value="Next">
        <#else>
            <input class="button" type="submit" name="Action" value="Next">
        </#if>
        <a href="<@url name="scan_result_export" args=[scanResult.scanResultID] />">[Export Results to CSV]</a>
        <#if ( contentTypeFilter?? )>
            <input type="hidden" name="ContentTypeFilter" value="${contentTypeFilter?html}">
        </#if>
        
        <#if ( scanRuleFilter?? )>
            <input type="hidden" name="RuleFilter" value="${scanRuleFilter?html}">
        </#if>
            <input type="hidden" name="S" value="${firstScanResultID?c}">
            <input type="hidden" name="E" value="${lastScanResultID?c}">
     </form>
     <br/>
</#assign>
<#include "BaseWithNav.ftl">
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#assign content>
    <div><span class="Text_1">HTTP Content Auto-Discovery Rule</span>
    <br><span class="LightText">Automatically crawls the website in order to identify malicious content</span><p/>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <#assign codepress><script type="text/javascript" language="javascript" src="/media/misc/codepress/codepress.js"></script></#assign>
    <#assign jquerytools><script type="text/javascript" language="javascript" src="/media/js/jquery.tools.min.js"></script></#assign>
    <#assign extrafooter=[codepress, jquerytools] />
    <script type="text/javascript">
        function submitEditorForm(editorform){
            document.editorform.StartAddresses2.value = cp1.getCode();
            document.editorform.submit();
            return true;
        }
        
        function getDomain (thestring) {
            //simple function that matches the beginning of a URL
            //in a string and then returns the domain.
            var urlpattern = new RegExp("(http|ftp|https)://(.*?)(/.*)?$");
            var parsedurl = thestring.match(urlpattern);
        
            if( parsedurl && parsedurl.length >= 2){
                return parsedurl[2];
            }
            else{
                return null;
            }
        }
        
        function stripSubDomain( url ){
            
            var parts = url.split(".");
            
            if( parts.length >= 2 ){
                return parts[parts.length-2] + "." + parts[parts.length-1] 
            }
            
            return null;
        }
        
        function getMinimalDomain( urls ){
            var coredomain = null;
            var domain = null;
            
            var allcoreDomainsMatch = true;
            var allDomainsMatch = true;
            
            for( var c = 0; c < urls.length; c++ ){
            
                var url = getDomain(urls[c]);
                
                //If the URL is null (did not parse), then do not bother processing it
                if( url != null ){
                    
                    // Set the baseline domain that will be used to perform the search
                    if( c == 0 ){
                        domain = url;
                        coredomain = stripSubDomain(url);
                    }
                    else{
                        
                        // Make sure the URL is not empty
                        if( $.trim( url ).length == 0 ){
                            //Ignore this, it is empty
                        }
                        
                        // Determine if the core domain is not equivalent
                        else if( stripSubDomain(url) != coredomain ){
                            allcoreDomainsMatch = false;
                            allDomainsMatch = false;
                        }
                        
                        // Determine if the sub-domain is not equivalent
                        else if( domain != url ){
                            allDomainsMatch = false;
                        }
                    }
                }
            }
            
            if( allDomainsMatch ){
                return domain;
            }
            else if(allcoreDomainsMatch){
                return coredomain;
            }
            else{
                return "";
            }
        }
        
        $(document).ready(function() {
            $("input[name=Domain]").focus( function() {
                
                if( $("input[name=Domain]").val().length == 0 ){
                
                    urls = cp1.getCode();
                    urls = urls.toLowerCase();
                    urls = urls.split("\n");
                    filter = getMinimalDomain(urls);
                    
                    if( filter == null ){
                        filter = "";
                    }
                    else if( filter.length == 0 ){
                        filter = "*";
                    }
                    else{
                        filter = "*" + filter + "*";
                    }
                    
                    $("input[name=Domain]").val(filter);
                }
            });
        });
        
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
        <input type="hidden" name="StartAddresses2" value="<#if request.getParameter("StartAddresses")??>${request.getParameter("StartAddresses")?html}<#elseif rule??><#list rule.seedUrls as url>${url}<@endline /></#list></#if>">
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
            <#-- 2 -- Output the start addresses -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("StartAddresses"))>ValidationFailed<#else>Background1</#if>">
                <td style="vertical-align: top;"><div style="margin-top: 5px;" class="TitleText">Addresses to Scan:</div></td>
                <td title="Define the URLs that the scanner should start from. NSIA will begin discovering the pages associated your web application from these URLs."><textarea id="cp1" class="codepress urls autocomplete-off" wrap="virtual" rows="11" cols="48" name="StartAddresses"><#if request.getParameter("StartAddresses")??>${request.getParameter("StartAddresses")?html}<#elseif request.getParameter("StartAddresses2")??>${request.getParameter("StartAddresses2")?html}<#elseif rule??><#list rule.seedUrls as url>${url?trim?html}<@endline /></#list></#if></textarea></td>
            </tr>
            <#-- 3 -- Output the domain limiter -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("Domain"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Domain</td>
                <td title="Enter the name of the domain you want to restrict scanning to (such as *threatfactor.com*)."><input class="textInput" size="40" type="text" name="Domain" value="<#if request.getParameter("Domain")??>${request.getParameter("Domain")?html}<#elseif rule??>${rule.domainRestriction?html}</#if>"></td>
            </tr>
            <#-- 4 -- Output the recursion depth -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("RecursionDepth"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Levels to Recurse</td>
                <td title="Limits how many levels deep the scanner will traverse"><input class="textInput" size="40" type="text" name="RecursionDepth" value="<#if request.getParameter("RecursionDepth")??>${request.getParameter("RecursionDepth")?html}<#elseif rule??>${rule.recursionDepth?c}<#else>10</#if>"></td>
            </tr>
            <#-- 5 -- Output the scan limit -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanLimit"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Maximum Number of Resource to Scan</td>
                <td title="Limits the number of resources scanned" ><input class="textInput" size="40" type="text" name="ScanLimit" value="<#if request.getParameter("ScanLimit")??>${request.getParameter("ScanLimit")?html}<#elseif rule??>${rule.scanCountLimit?c}</#if>"></td>
            </tr>
            <#-- 6 -- Output whether to scan external URLs -->
            <tr class="<#if (form_errors?? && form_errors.fieldHasError("ScanExternalURLs"))>ValidationFailed<#else>Background1</#if>">
                <td class="TitleText">Scan External Linked Content</td>
                <td title="Define whether or not the first level of external URLs will be scanned (to scan the pages on the external sites you link to)"><input name="ScanExternalURLs" type="checkbox" <#if rule??><#if rule.scansExternalLinks>checked</#if><#elseif request.getParameter("DontScanExternalURLs")??><#else>checked</#if>></td>
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
         <p/>
</#assign>
<#include "BaseWithNav.ftl">
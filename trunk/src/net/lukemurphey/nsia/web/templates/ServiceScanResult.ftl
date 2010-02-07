<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>
    <#macro summaryrow name value>
        <tr class="Background1"><td class="Text_3">${name}</td><td class="Text_3">${value}</td></tr>
    </#macro>
        <#-- <span class="Text_1">Scan Result</span>
        <br><span class="LightText">Viewing service scan result</span><p> -->
        
        <table cellpadding="2">
            <tr class="Background0">
                <td colspan="2" class="Text_3">Scan Parameters</td>
            </tr>
            <@summaryrow "Deviations" scanResult.deviations />
            <#assign scannedports>
                ${scanResult.accepts + scanResult.deviations - scanResult.incompletes}
            </#assign>
            <@summaryrow "Ports Scanned" scannedports />
            <@summaryrow "Server" scanResult.specimenDescription />
        </table><p/>
        
        <#-- 1 -- Print out list of violations -->
        <table>
            <tr class="Background0">
                <td colspan="2" class="Text_3">Deviations</td>
            </tr>
        <#--   1.1 -- Print out graph -->
            <tr class="Background1">
                <td width="125px">
                    <img src="/ServiceScanDeviations?ResultID=${scanResult.scanResultID?c}&H=125"/>
                </td>
        <#--   1.2 -- Print out details -->
                <td width="325px">
            <#list scanResult.differences as diff>
                <#if (diff.state == OPEN )>
                    <div style="background-image: url('/media/img/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left">
                <#else>
                    <div style="background-image: url('/media/img/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left">
                </#if>
                ${diff}<br/>[${diff.state?lower_case}]</div>
            </#list>
                </td>
            </tr>
        </table>
        
        <#-- 2 -- Print out TCP results -->
        <table>
            <tr class="Background0">
                <td colspan="2" class="Text_3">TCP Overview</td>
            </tr>
        
        <#--   2.1 -- Print out graph -->
            <tr class="Background1">
                <td width="125px">
                    <img src="/TCPSummary?ResultID=${scanResult.scanResultID?c}&H=125"/>
                </td>
        
        <#--   2.2 -- Print out details -->
                <td width="325px">
            <#list scanResult.portsScanned as scanned>
                <#if ( scanned.protocol == TCP)>
                    <#if ( scanned.state == OPEN )>
                        <div style="background-image: url('/media/img/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left">
                    <#else>
                        <div style="background-image: url('/media/img/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left">
                    </#if>
                    ${scanned}<br/>[${scanned.state?lower_case}]</div>
                </#if>
            </#list>
                </td>
            </tr>
        </table>
        
        <#-- 3 -- Print out UDP results -->
        <table>
            <tr class="Background0">
                <td colspan="2" class="Text_3">UDP Overview</td>
            </tr>
        
        <#--   3.1 -- Print out graph -->
            <tr class="Background1">
                <td width="125px">
                    <img src="/UDPSummary?ResultID=${scanResult.scanResultID?c}&H=125"/>
                </td>
        
        <#--   3.2 -- Print out details -->
                <td width="325px">
            <#list scanResult.portsScanned as scanned>
                <#if ( scanned.protocol == UDP)>
                    <#if ( scanned.state == OPEN )>
                        <div style="background-image: url('/media/img/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left">
                    <#else>
                        <div style="background-image: url('/media/img/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left">
                    </#if>
                    ${scanned}<br/>[${scanned.state?lower_case}]</div>
                </#if>
            </#list>
                </td>
            </tr>
        </table>
</#assign>
<#include "BaseWithNav.ftl">
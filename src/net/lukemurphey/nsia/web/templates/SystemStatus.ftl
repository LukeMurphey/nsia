<#assign content>

<#macro getWarningNote message>
    <table style="float:left">
        <tr>
            <td><img src="/media/img/16_Warning" alt="Warning"></td>
            <td class="WarnText">
            ${message?html}
            </td>
        </tr>
    </table>
</#macro>

<#macro getCheckNote message>
    <table style="float:left">
        <tr>
            <td><img src="/media/img/16_Check" alt="OK"></td>
            <td>
            ${message?html}
            </td>
        </tr>
    </table>
</#macro>

<#macro getErrorNote message>
    <table style="float:left">
        <tr>
            <td><img src="/media/img/16_Alert" alt="Alert"></td>
            <td class="WarnText">
            ${message?html}
            </td>
        </tr>
    </table>
</#macro>

<#macro getInfoNote message>
    <table style="float:left;">
        <tr>
            <td><img src="/media/img/16_Information" alt="Info"></td>
            <td>
            ${message?html}
            </td>
        </tr>
    </table>
</#macro>

<#macro getattrs attrs>
    <#if attrs??>
        <#list attrs as attr>
            ${attr.name}="${attr.value}"
        </#list> 
    </#if>
</#macro>

<#macro getRow stat>
    <tr>
        <td width="300" class="Background1">
            <div class="Text_3">
            ${stat.title?html}
            </div>
        </td>
        <td class="Background1">
            <#if stat.warning>
                <@getWarningNote message=stat.message />
            <#elseif stat.check>
                <@getCheckNote message=stat.message />
            <#elseif stat.info>
                <@getInfoNote message=stat.message />
            <#elseif stat.error>
                <@getErrorNote message=stat.message />
            </#if>
            
            <#if stat.href??>
                <table><tr><td><a <@getattrs stat.href.attributes /> href="${stat.href.link?html}">${stat.href.title?html}</a></td></tr></table>
            </#if>
        </td>
    </tr>
</#macro>

<div class="SectionHeader">Operational Metrics</div>
<table width="640px" class="DataTable">
<#list system_stats as stat>
    <@getRow stat=stat />
</#list>
    <tr>
        <td height="120px" colspan="2" class="BackgroundLoading1">
            <span class="AlignCenter"><img src="/graph/StatusGraph?t=${current_time_millis?c}" alt="StatusGraph"></span>
        </td>
    </tr>
    <tr>
        <td height="120px" colspan="2" class="BackgroundLoading1">
            <span class="AlignCenter"><img src="/graph/RulesEvalGraph?t=${current_time_millis?c}" alt="RuleStatGraph"></span>
        </td>
    </tr>
</table><p>

<div class="Text_2">License</div>
<table width="640px" class="DataTable">
<#list license_stats as stat>
    <@getRow stat=stat />
</#list>
</table><p>

<#if log_server_stats??>
<div class="Text_2">External Log Server</div>
<table width="640px" class="DataTable">
<#list log_server_stats as stat>
    <@getRow stat=stat />
</#list>
</table><p>
</#if>

<div class="Text_2">Configuration</div>
<table width="640px" class="DataTable">
<#list configuration_stats as stat>
    <@getRow stat=stat />
</#list>
</table><p>

</#assign>
<#include "BaseWithNav.ftl">
<#assign content>
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<span class="Text_1">Event Log</span><br>&nbsp;
<form action="<@url name="event_log" />" method="get">
    <table>
        <tr class="Background0">
            <td colspan="99"><span class="Text_3">Log Entry Filter</span></td>
        </tr>
        <tr class="Background1">
            <td><span class="Text_3">Severity Filter:</span></td><td>Display all entries that are at least as severe as
                <select name="Severity">
                    <option <#if severity == -1>selected</#if> value="-1"></option>
                    <option <#if severity == 0>selected</#if> value="0">Emergency</option>
                    <option <#if severity == 1>selected</#if> value="1">Alert</option>
                    <option <#if severity == 2>selected</#if> value="2">Critical</option>
                    <option <#if severity == 3>selected</#if> value="3">Error</option>
                    <option <#if severity == 4>selected</#if> value="4">Warning</option>
                    <option <#if severity == 5>selected</#if> value="5">Notice</option>
                    <option <#if severity == 6>selected</#if> value="6">Informational</option>
                    <option <#if severity == 7>selected</#if> value="7">Debug</option>
                </select>
            </td>
        </tr>
        <tr class="Background1">
            <td>
                <span class="Text_3">Content Filter:</span>
            </td>
            <td>Display all entries that contain <input class="textInput" type="Text" name="Content" value="<#if contentfilter?? >${contentfilter}</#if>"></td>
        </tr>
        <tr class="Background3">
            <td colspan="99"><input class="button" type="Submit" name="Apply" value="Apply"></td>
        </tr>
        <tr>
            <td colspan="99" style="height:10px"></td>
        </tr>
        <#if (entries?size == 0)>
            <#if (severity <= -1 && contentFilter??)>
            <@getinfodialog title="No Log Entries" message="No log entries exist yet" />
            <#else>
            <@getinfodialog title="No Log Entries Match" message="No log entries exist for the given filter." />
            </#if>
        <#else>
        <table width="100%">
            <tr class="Background0">
                <td colspan="2"><span class="Text_3">Severity</span></td>
                <td><span class="Text_3">Time</span></td>
                <td><span class="Text_3">Event ID</span></td>
                <td><span class="Text_3">Message</span></td>
                <td><span class="Text_3">Notes</span></td>
            </tr>
        <#list entries?reverse as entry >
            <tr class="Background1">
            <#if (entry.severity == emergency)>
                <td class="StatRedSmall"><span style="vertical-align: middle;"><img src="/16_Alert" alt="Emergency"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == alert) />
                <td class="StatRedSmall"><span style="vertical-align: middle;"><img src="/16_Alert" alt="Alert"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == critical) />
                <td class="StatRedSmall"><span style="vertical-align: middle;"><img src="/16_Alert" alt="Critical"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == error) />
                <td class="StatYellowSmall"><span style="vertical-align: middle;"><img src="/16_Warning" alt="Error"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == warning) />
                <td class="StatYellowSmall"><span style="vertical-align: middle;"><img src="/16_Warning" alt="Warning"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == notice) />
                <td class="StatGreenSmall"><span style="vertical-align: middle;"><img src="/16_Check" alt="Notice"></span></td>
                <td>${entry.severity}</td>
            <#elseif (entry.severity == informational) />
                <td class="StatBlueSmall"><span style="vertical-align: middle;"><img src="/16_Information" alt="Informational"></span></td>
                <td>${entry.severity}</td>
            <#else>
                <td class="StatBlueSmall"><span style="vertical-align: middle;"><img src="/16_Information" alt="Informational"></span></td>
                <td>${entry.severity}</td>
            </#if>
                <td>${entry.date}</td>
                <td><a href="<@url name="event_log_entry" args=[entry.entryID] /><#if (severity >= 0 || contentFilter?? )>?<#if (severity >= 0)>Severity=${severity}&</#if><#if (contentFilter??)>Content=${contentFilter}&</#if></#if>">[${entry.entryID}]</a></td>
                <td><@truncate_chars length=70>${entry.message}</@truncate_chars></td>
                <td><@truncate_chars length=40>${entry.notes}</@truncate_chars></td>
            </tr>
        </#list>
        </table><p/>
        
        <form action="<@url name="event_log" />" method="get">
            <#if hasnext>
                <input class="button" type="Submit" name="Action" value="[Next]">
            <#else>
                <input disabled class="buttonDisabled" type="Submit" name="Action" value="[Next]">
            </#if>
            
            <#if hasprev>
                <input class="button" type="Submit" name="Action" value="[Previous]">
            <#else>
                <input disabled class="buttonDisabled" type="Submit" name="Action" value="[Previous]">
            </#if>
            
            <#if contentfilter?? >
                <input class="button" type="hidden" name ="Content" value="<#if contentfilter?? >${contentfilter}</#if>">
            </#if>
            
            <#if (severity?? && severity >= 0) >
                <input class="button" type="hidden" name ="Severity" value="${severity}">
            </#if>
            
            <#function increment x >
                <#return x+1>
            </#function>
            
            <input class="button" type="hidden" name="Start" value="${increment(entries?last.entryID)?c}">
            <input class="button" type="hidden" name="PrevID" value="${entries?first.entryID?c}">
        </#if>

</#assign>
<#include "BaseWithNav.ftl" />
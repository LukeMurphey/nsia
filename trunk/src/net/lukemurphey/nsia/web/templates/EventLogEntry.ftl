<#assign content>
<#include "GetURLFunction.ftl" />
<#include "GetDialog.ftl" />
<div style="position:relative;" />
<#if (entry.severity == emergency) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Alert" alt="Emergency"></div>
<#elseif (entry.severity == alert) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Alert" alt="Alert"></div>
<#elseif (entry.severity == critical) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Alert" alt="Critical"></div>
<#elseif (entry.severity == error) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Warning" alt="Error"></div>
<#elseif (entry.severity == warning) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Warning" alt="Warning"></div>
<#elseif (entry.severity == notice) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Check" alt="Notice"></div>
<#elseif (entry.severity == informational) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Information" alt="Info"></div>
<#elseif (entry.severity == debug) >
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Information" alt="Debug"></div>
<#else>
    <div style="position:absolute; left:0px;"><img src="/media/img/32_Information" alt="Info"></div>
</#if>
<div style="position: relative; left: 32px;">
    <table cellpadding="2">
        <tr><td class="Text_2">Event</td>
            <td>${entry.message?html}</td>
        </tr>
        <tr>
            <td class="Text_2">Severity</td>
            <td>${entry.severity?html}</td>
        </tr>
        <tr>
            <td class="Text_2">Date</td>
            <td>${entry.formattedDate}</td>
        </tr>
        <#if entry.notes??>
        <tr>
            <td class="Text_2">Notes</td>
            <td>${entry.notes?html}</td>
        </tr>
        </#if>
        </table><p>
        <a href="<@url name="event_log" />">[Return to Event Log List]</a><p>
        
        <table>
            <tr>
                <td>
                <#if curPrevId?? && (curPrevId >= 0)>
                    <form method="post" action="<@url name="event_log_entry" args=[curPrevId] />">
                    <input class="button" type="Submit" name="Action" value="[Previous]" />
                    <#if (severity >= 0)><input type="hidden" name="Severity" value="${severity?html}" /></#if>
                    <#if ( contentFilter?? )><input type="hidden" name="Content" value="${contentFilter?html}" /></#if>
                    <input type="hidden" name="PrevEntryID" value="${curPrevId?c}" />
                    </form>
                <#else>
                    <input disabled class="buttonDisabled" type="Submit" name="Action" value="[Previous]" />
                </#if>
                </td>
                <td>
                <#if curNextId?? && (curNextId >= 0)>
                    <form method="post" action="<@url name="event_log_entry" args=[curNextId] />">
                    <input class="button" type="Submit" name="Action" value="[Next]" />
                    <#if (severity >= 0)><input type="hidden" name="Severity" value="${severity?html}" /></#if>
                    <#if ( contentFilter?? )><input type="hidden" name="Content" value="${contentFilter?html}" /></#if>
                    <input type="hidden" name="EntryID" value="${curNextId?c}" />
                    </form>
                <#else>
                    <input disabled class="buttonDisabled" type="Submit" name="Action" value="[Next]" />
                </#if>
                </td>
          </tr>
       </table>
    </div>
</div>
</#assign>
<#include "BaseWithNav.ftl" />
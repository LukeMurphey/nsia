<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#if (definitions?size == 0 && !filter?? )>
    <#assign message>
        No definitions exist yet. Download updated definitions to get the most current official set.<p><a href="<@url name="definitions_update" />">[Update Definitions Now]</a>
    </#assign>
    <@getinfodialog title="No Definitions" message=message />
<#elseif (definitions?size == 0)>
    <#assign message>
        No definitions match the filter <p><a href="<@url name="definitions_list" />">[Clear Filter]</a>
    </#assign>
    <@getinfodialog title="No Matches Found" message=message />
<#else>
    <#if updated?? >
    <span class="Text_1">Definitions</span><br />
    Revision ${definition_set_version} (Last Updated ${updated?datetime})<br>&nbsp;<br/>
    <#else>
    <span class="Text_1">Definitions</span><br/>
    ${definitions?size} definitions total (Last Update Undefined)<br>&nbsp;<br/>
    </#if>
    
    <#if newer_definitions_available >
        <#assign message>
            Newer definitions are available (version ${latest_definitions}<a href="<@url name="definitions_list" />">[Update Now]</a><p/>
        </#assign>
        <@getinfodialog title="Current Definitions Obsolete" message=message />
    </#if>
    
    <form method="get" action="<@url name="definitions_list" />"><input class="button" type="Submit" value="Filter">
        <input class="textInput" type="text" width="32" name="Filter" value="<#if filter??>${filter}</#if>" />
        <input id="NotFilter" type="checkbox" name="Not" <#if not_filter>checked</#if>><label for="NotFilter">Exclude (excludes items based on the filter)</label></input>
    </form>
    
    
    <table class="DataTable">
        <thead>
            <tr>
                <td>Name</td>
                <td>Type</td>
                <td colspan="2">Options</td>
            </tr>
        </thead>
        <#list definitions as def>
            <#if is_even??>
                <tr class="even">
            <#else>
                <tr class="odd">
            </#if>
            
            <#if def.type = "ThreatScript" >
                <td>&nbsp;<img style="vertical-align: top;" src="/media/img/16_script" alt="script">&nbsp;${def}&nbsp;&nbsp;&nbsp;</td>
            <#else>
                <td>&nbsp;<img style="vertical-align: top;" src="/media/img/16_plugin" alt="script">&nbsp;${def}&nbsp;&nbsp;&nbsp;</td>
            </#if>
            
            <#if def.official >
                <td>Official&nbsp;&nbsp;</td>
                <td colspan="2">&nbsp;&nbsp;<a href="<@url name="definition" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_magnifier" alt="View"><span style="vertical-align:top;">&nbsp;View</span></a>&nbsp;</td>
            <#else>
                <td>Local&nbsp;&nbsp;</td>
                <td>&nbsp;&nbsp;<a href="<@url name="definition" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_Configure" alt="Edit"><span style="vertical-align:top;">&nbsp;Edit</span></a>&nbsp;</td>
                <td>&nbsp;&nbsp;<a href="<@url name="definition_delete" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_Delete" alt="Delete"><span style="vertical-align:top;">&nbsp;Delete</span>&nbsp;</a></td>
            </#if>
        </tr>
        </#list>     
    </table>
    
    <#if ( total_entries >= entries_per_page)>
        
        <form action="/Definitions" method="Post">
            <#if ( start_page > 1 )>
                <input style="width:64px;" class="button" type="submit" name="N" value="[Start]">&nbsp;
            </#if>
            
            <#list page_numbers as n>
                <input style="width:32px;" class="button" type="submit" name="N" value="${n}" />
            </#list>
            
            <#if needs_end_marker??>
                <input style="width:64px;" class="button" type="submit" name="N" value="[End]" />
            </#if>
            
            <#if filter?? >
                <#if not_filter?? >
                <input type="hidden" name="Not" value="Not">
                </#if>
                <input type="hidden" name="Filter" value="${filter}">
            </#if>
        </form>
    </#if>
</#if>
<p/>
</#assign>
<#include "BaseWithNav.ftl">
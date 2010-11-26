<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
<#include "PopupDialog.ftl">
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
    Revision ${definition_set_version?html} (Last Updated ${updated?datetime})
    <#else>
    <span class="Text_1">Definitions</span><br/>
    ${definitions?size} definitions total (Last Update Undefined)
    </#if>
    
    <#if newer_definitions_available >
    <table>
        <tr> 
            <td style="vertical-align:top"><img src="/media/img/16_Warning" /></td>
            <td><span class="WarnText">Newer definitions available,</span> <a href="<@url name="definitions_update" />">[Update to version ${latest_definitions?html}]</a></td>
        </tr>
    </table>
    <br>
    
    <#else>
    <br>&nbsp;<br/>
    </#if>
    
    <form method="get" action="<@url name="definitions_list" />"><input class="button" type="Submit" value="Filter">
        <input class="textInput" type="text" width="32" name="Filter" value="<#if filter??>${filter?html}</#if>" />
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
                <td>&nbsp;<img style="vertical-align: top;" src="/media/img/16_script" alt="script">&nbsp;${def?html}&nbsp;&nbsp;&nbsp;</td>
            <#else>
                <td>&nbsp;<img style="vertical-align: top;" src="/media/img/16_plugin" alt="script">&nbsp;${def?html}&nbsp;&nbsp;&nbsp;</td>
            </#if>
            
            <#if def.official >
                <td>Official&nbsp;&nbsp;</td>
                <td colspan="2">&nbsp;&nbsp;<a href="<@url name="definition" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_magnifier" alt="View"><span style="vertical-align:top;">&nbsp;View</span></a>&nbsp;</td>
            <#else>
                <td>Local&nbsp;&nbsp;</td>
                <td>&nbsp;&nbsp;<a id="edit" href="<@url name="definition" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_Configure" alt="Edit"><span style="vertical-align:top;">&nbsp;Edit</span></a>&nbsp;</td>
                <td>&nbsp;&nbsp;<a id="delete" href="<@url name="definition_delete" args=[def.ID] />"><img class="imagebutton" src="/media/img/16_Delete" alt="Delete"><span style="vertical-align:top;">&nbsp;Delete</span>&nbsp;</a></td>
            </#if>
        </tr>
        </#list>     
    </table>
    
    <#if ( total_entries >= entries_per_page)>
        
        <form action="<@url name="definitions_list" />" method="Post">
            <#if ( start_page > 1 )>
                <input style="width:64px;" class="button" type="submit" name="N" value="Start">&nbsp;
            </#if>
            
            <#list page_numbers as n>
                <input style="width:32px;" class="button" type="submit" name="N" value="${n?c}" />
            </#list>
            
            <#if needs_end_marker??>
                <input style="width:64px;" class="button" type="submit" name="N" value="End" />
            </#if>
            
            <#if filter?? >
                <#if not_filter?? >
                <input type="hidden" name="Not" value="Not">
                </#if>
                <input type="hidden" name="Filter" value="${filter?html}">
            </#if>
        </form>
    </#if>
</#if>
<p/>
<script type="text/javascript">
$(document).ready(
    function(){
        $('#delete').click(
            function(){
                openDeleteConfirmDialog( "Are you sure you want to delete this definition? This action cannot be undone.", "Delete Definition?", this.href );
                return false;
             }
         );
    }
);
</script>            
</#assign>
<#include "BaseWithNav.ftl">
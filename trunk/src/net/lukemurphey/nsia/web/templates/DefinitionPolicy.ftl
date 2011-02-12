<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#macro policyrow category >
    <tr>
    <td width="6"><input class="selectable" type="checkbox" name="DefinitionPolicy" value="${category.name?html}"></td>
    <#if category.enabled >
        <td width="40" align="center" class="StatGreen"><img src="/media/img/22_Check" alt="ok"></td>
    <#else>
        <td width="40" align="center" class="StatRed"><img src="/media/img/22_Alert" alt="alert"></td>
    </#if>
        <td>${category.name?html}</td>
        
    <#if category.enabled >
        <td>Enabled
    <#else>
        <td>Disabled
    </#if>
        
    <#if (sitegroup?? && category.default) >
         (inherited from <a href="<@url name="definitions_policy" />">default policy</a>)
    </#if>
        </td>
    </tr>
</#macro>

<#assign content>

<#if categories?size = 0 >
    <#assign message>
        No definitions exist yet. Download updated definitions to get the most current official set.<p><a href="<@url name="definitions_update" />">[Update Definitions Now]</a>
    </#assign>
    <@getinfodialog title="No Definitions" message=message />
<#else>
<span class="Text_1">Scan Policy Management</span>
<#if (sitegroup??)>
<br><span class="LightText">Viewing scan policy for site-group ${sitegroup.groupName?html}</span>
<#else>
<br><span class="LightText">Viewing default scan policy</span>
</#if>
<p>
<#include "SelectAll.ftl">
<#include "PopupDialog.ftl">
<form id="policyeditform" action="${request.thisURL?html}" method="post" action="ScanPolicy">
<#if (sitegroup??)>
    <input type="hidden" name="SiteGroupID" value="${sitegroup.groupId?c}">
</#if>
    <table class="DataTable" width="80%" summary="Definition categories">
        <thead>
            <tr>
                <td colspan="4">
                    <div style="float:left">
                        <input type="checkbox" id="selectall">
                    </div>
                    <div>Category</div>
                </td>
            </tr>
        </thead>
    <#list categories as category>
        <@policyrow category=category />
    </#list>
        <tr class="lastRow">
            <td colspan="4">
                <input type="submit" class="button" name="Action" value="Disable">
                &nbsp;<input type="submit" class="button" name="Action" value="Enable">
        <#if sitegroup??>
                &nbsp;<input type="submit" class="button" name="Action" value="Set Default">
        </#if>
            </td>
        </tr>            
    </table>
</form>
<script type="text/javascript">
    $(document).ready(
        function(){
            $('#policyeditform').submit(
                function(){
                    var count = $('input.selectable:checked').length;
                    if( count == 0 ){
                        openDialog("No categories are selected. Please select at least one category first.", "No Categories Selected");
                        return false;
                    }
                }
             );
        }
    );
</script>
</#if>
</#assign>
<#include "BaseWithNav.ftl">
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>
    <div class="Text_1">Select Response Type</div>
    <form action="<@url name="sitegroup_action_editor" args=["New"] />" method="get">
        <#list extensions as extension>
        <input type="radio" name="Extension" value="${extension.name?html}"/>${extension.description?html}<br>
        </#list>
        <input type="hidden" name="SiteGroupID" value="${siteGroup.groupId?c}">
        <input type="hidden" name="Action" value="New">
        &nbsp;<p/><input class="button" value="Create" type="submit">
        &nbsp;<input class="button" name="Cancel" value="Cancel" type="submit">
    </form>
</#assign>
<#include "BaseWithNav.ftl">
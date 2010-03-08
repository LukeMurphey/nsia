<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#assign content>
    <span class="Text_1"><#if isUser>User<#else>Group</#if> Rights Management</span><p />
    <#assign rightsurl><#if isUser><@url name="rights_editor" args=["User", user.userID] /><#else><@url name="rights_editor" args=["Group", group.groupId] /></#if></#assign>
    <ul id="NavigationTabs">
        <li class="Tab1"><a href="${rightsurl}?TabIndex=${USER_MANAGEMENT}">User Management</a></li>
        <li class="Tab2"><a href="${rightsurl}?TabIndex=${GROUP_MANAGEMENT}">Group Management</a></li>
        <li class="Tab3"><a href="${rightsurl}?TabIndex=${SITE_GROUP_MANAGEMENT}">Site Group Management</a></li>
        <li class="Tab4"><a href="${rightsurl}?TabIndex=${SYSTEM_CONFIGURATION}">System Configuration</a></li>
    </ul>
    
    <form action="${rightsurl}" method="post">
        <div class="TabPanel">
            <#list rights as right>
                <span class="SpacedInput">
                <input id="${right.name?html}" type="checkbox" name="${right.name?html}"<#if right.permitted> checked</#if> />
                <label for="${right.name?html}">${right.description?html}</label>
                <br>
                </span>
            </#list>
        </div>
        <p/>
        <input class="button" type="Submit" value="Apply" name="Apply" />
        <input type="hidden" value="${tabIndex?c}" name="TabIndex">
     </form>
</#assign>
<#include "BaseWithNav.ftl">
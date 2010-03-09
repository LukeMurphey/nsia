<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#assign content>
    <span class="Text_1"><#if isUser>User<#else>Group</#if> Rights Management</span><p />
    <#assign rightsurl><#if isUser><@url name="rights_editor" args=["User", user.userID] /><#else><@url name="rights_editor" args=["Group", group.groupId] /></#if></#assign>
    
    <script type="text/javascript">
    $(function() {
        $("#tabs").tabs();
    });
    </script>

    <form action="${rightsurl}" method="post">
    <div id="tabs">
    <ul>
        <#list categories as category>
        <#-- <li><a href="${rightsurl}?TabIndex=${category.index}">${category.name}</a></li> -->
        <li><a href="#tabs-${category.index}">${category.name}</a></li>
        </#list>
    </ul>
    <#list categories as category>
        <div id="tabs-${category.index}">
        <#list category.rights as right>
            <span class="SpacedInput">
            <input id="${right.name?html}" type="checkbox" name="${right.name?html}"<#if right.permitted> checked</#if> />
            <label for="${right.name?html}">${right.description?html}</label>
            <br>
            </span>
        </#list>
        </div>
    </#list>
    
    <input class="button" style="margin-left: 32px;margin-bottom: 16px" type="Submit" value="Apply" name="Apply" />
    <input type="hidden" value="${tabIndex?c}" name="TabIndex">
    
    </div>
    <#-- <div id="tabs-1">
    
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
     </form> -->
</#assign>
<#include "BaseWithNav.ftl">
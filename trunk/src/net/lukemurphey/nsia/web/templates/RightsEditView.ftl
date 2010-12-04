<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#assign content>
    <span class="Text_1"><#if isUser>User<#else>Group</#if> Rights Management</span><p>
    <#assign rightsurl><#if isUser><@url name="rights_editor" args=["User", user.userID] /><#else><@url name="rights_editor" args=["Group", group.groupId] /></#if></#assign>
    <style>
    </style>
    
    <#assign jquerytools><script type="text/javascript" language="javascript" src="/media/js/jquery.tools.min.js"></script></#assign>
    <#assign tabs><link rel=StyleSheet href="/media/css/Tabs.css"></#assign>
    <#assign extrahead=[tabs] />
    <#assign extrafooter=[jquerytools] />
    
    <form action="${rightsurl}" method="post">

    <div class="wrap"> 
        <ul class="tabs">
            <#list categories as category>
            <li><a class="w2" href="#tabs-${category.index?html}">${category.name?html}</a></li>
            </#list>
        </ul>
        
        <#list categories as category>
            <div class="pane">
            <#list category.rights as right>
                <span class="SpacedInput">
                    <input id="${right.name?html}" type="checkbox" name="${right.name?html}"<#if right.permitted> checked</#if> />
                    <label for="${right.name?html}">${right.description?html}</label>
                    <br>
                </span>
            </#list>
            <input class="button" style="margin-left: 32px;margin-top: 8px" type="Submit" value="Apply" name="Apply" />
            </div>
            
        </#list>
                
    </div>
    </form>
    <script type="text/javascript">
        $(function() {
            $("ul.tabs").tabs("> .pane");
        });
    </script>
</#assign>
<#include "BaseWithNav.ftl">
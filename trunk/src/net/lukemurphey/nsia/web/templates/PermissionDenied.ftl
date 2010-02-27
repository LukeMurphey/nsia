<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">

<#assign content>
    <#assign message>
        <#if permission_denied_message??>
            ${permission_denied_message?html}
        <#else>
            You do not have sufficient permission.
        </#if>
        
        <#if permission_denied_link??>
            <p /><a href="${permission_denied_link.link?html}">[${permission_denied_link.title?html}]</a>
        <#else>
            <p /><a href="<@url name="main_dashboard" />">[Return to the Main Dashboard]</a>
        </#if>
    </#assign>
    
    <#assign title>
        <#if permission_denied_title??>
            ${permission_denied_title?html}
        <#else>
            Permission Denied
        </#if>
    </#assign>
    
    <@getwarndialog title=title message=message />
</#assign>
<#if show_simple><#include "Basic.ftl"><#else><#include "BaseWithNav.ftl"></#if>
<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">

<#assign content>
    <#macro rulerow name description sitegroupID ruletype>
        <div style="padding:5px;">
            <div class="ToolIcon" style="float:left;"><a href="<@url name="rule_editor" args=["New"] />?SiteGroupID=${sitegroupID}&RuleType=${ruletype}"><img src="/media/img/32_Add"></a></div>
            <div style="position:relative; left:8px;"><a href="<@url name="rule_editor" args=["New"] />?SiteGroupID=${sitegroupID}&RuleType=${ruletype}"><span class="Text_2">${name}</span></a><br>${description}</div>
            <br>
        </div>
    </#macro>
    <div style="padding:5px;"><span class="Text_2">Select Type of Rule To Add<span></div>
    
    <@rulerow name="Service Monitoring" description="Analyses open ports on a server and alerts if new ports open or exiting ports close" sitegroupID=siteGroupID ruletype="Service Scan"/>
    <@rulerow name="HTTP Content Auto-Discovery" description="Automatically discovers HTTP content and analyzes it for rogue content. This rule will automatically discover the context associated with your website." sitegroupID=siteGroupID ruletype="HTTP/Autodiscovery"/>
    <#-- <@rulerow name="Static HTTP Content" description="Analyses HTTP content that is not expected to change often and alerts if the content changes" sitegroupID=siteGroupID ruletype="Static"/> -->
</#assign>
<#include "BaseWithNav.ftl">
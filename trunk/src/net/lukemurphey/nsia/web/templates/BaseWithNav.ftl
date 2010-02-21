<#macro getattrs attrs>
	<#if attrs??>
		<#list attrs as attr>
			${attr.name}=${attr.value}
		</#list> 
	</#if>
</#macro>
<#assign content>
    <table cellspacing="0" align="left" width="95%">
        <tr>
            <td rowspan="3" class="Menu" width="220px" valign="top">
            <#list menu as menu_item>
                
                <#if menu_item.link??>
                <br>&nbsp;&nbsp;&nbsp;<img alt="*" src="/media/img/Arrow" /><a href="${menu_item.link}" <@getattrs menu_item.attributes />>${menu_item.title?html}</a>
                <#else>
                <br>&nbsp;
                <#if (menu_item_index > 0)><br>&nbsp;</#if>
                ${menu_item.title?html}
                </#if>
            </#list>
            </td>
            <td rowspan="3" width="16">&nbsp;</td>
            <td valign="top">            
            <div style="margin-bottom: 16px;" class="BottomBorder"><br>
            <#list breadcrumbs as crumb>
            <#if (crumb_index > 0)>/</#if> <#if crumb.link??><a class=NavBar href="${crumb.link}"></#if>${crumb.title?html}<#if crumb.link??></a></#if>
            </#list>
            &nbsp;</div>
            <#if (context??)>
                <#macro message htmlclass icon message >
                <table>
                    <tr>
                        <td><img src="${icon}" /></td>
                        <td class="${htmlclass}">${message?html}</td>
                    </tr>
                </table>
                </#macro>
                <#list context.messages as m>
                    <#if m.alert>
                        <@message htmlclass="WarnText" icon="/media/img/16_Alert" message=m.messageAndDelete />
                    <#elseif m.informational>
                        <@message htmlclass="InfoText" icon="/media/img/16_Information" message=m.messageAndDelete />
                    <#elseif m.success>
                        <@message htmlclass="InfoText" icon="/media/img/16_Information" message=m.messageAndDelete />
                    <#else>
                        <@message htmlclass="WarnText" icon="/media/img/16_Warning" message=m.messageAndDelete />
                    </#if>
                </#list>
            </#if>
            <div>
            ${content}
            </div>
            </td>
        </tr>
   </table>
</#assign>
<#include "Base.ftl">
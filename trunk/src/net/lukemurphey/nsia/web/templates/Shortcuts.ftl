<#macro iconlink url imageURL imageAlt text>
<table><tr><td><a href="${url}"><img class="imagebutton" src="${imageURL}" alt="${imageAlt?html}"></a></td><td><a href="${url}">${text?html}</a></td></tr></table>
</#macro>
<#macro endline>

</#macro>
<#macro infonote message >
    <table><tr><td style="vertical-align: top;"><img src="/media/img/16_Information" alt="Info"/></td>
    <td class="InfoText">${message?html}<td></tr></table>
</#macro>
<#macro warningnote message >
    <table><tr><td><img src="/media/img/16_Warning" alt="Warning"></td>
    <td class="WarnText">${message?html}<td></tr></table>
</#macro>
<#macro message htmlclass icon message >
<table>
	<tr>
		<td><img src="${icon}" /></td>
		<td class="${htmlclass}">${message?html}</td>
	</tr>
</table>
</#macro>
<#macro usermessages context >
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
</#macro>
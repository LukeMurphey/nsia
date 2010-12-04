<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#assign content>
	<#assign message>
	${login_banner?replace("\n", "<br/>")}
	<p>
		<form method="post" action="${request.thisURL?html}">
			<input class="button" type="submit" value="Accept" name="BannerCheck">
			<#if request.getParameter("ReturnTo")??><input class="hidden" type="hidden" value="${request.getParameter("ReturnTo")?html}" name="ReturnTo"></#if>
		</form><p/>
	</#assign>
	<div style="width:50%;margin-left: auto;margin-right: auto;margin-top:60px;margin-bottom:60px;">
	<@getinfodialog title="Use Subject to Monitoring" message=message />
	</div>
</#assign>
<#include "Base.ftl">
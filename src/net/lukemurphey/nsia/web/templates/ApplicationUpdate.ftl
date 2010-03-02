<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#assign content>
	<p/><div style="width: 600px">
	<#if ( !new_version?? )>
		<#assign message>
			The application could not determine if this version of the application is the most current. This may be because an Internet connection to ThreatFactor.com is unavailable.
			<p/>To get the newest version of NSIA, please go to <a href="http://ThreatFactor.com/Support">ThreatFactor.com</a>.
			<p/>
			<form method="POST" action="/"><input type="Submit" class="button" value="OK" name="OK"></form>
		</#assign>
		<@getwarndialog title="Version Check Failed" message=message/>
	<#elseif !is_newer >
		<#assign message>
			This application is the most current version (${version?html}).
			<p/>Go to <a href="http://ThreatFactor.com/Support">ThreatFactor.com</a> for more information.
			<p/><form method="POST" action="/"><input type="Submit" class="button" value="OK" name="OK"></form>
		</#assign>
		<@getinfodialog title="NSIA Up To Date" message=message/>
	<#else>
		<#assign message>
			A new version (${new_version?html}) of the application is available. You are currently using version ${version?html}.
			<p/>To update, please go to <a href="http://ThreatFactor.com/Support">ThreatFactor.com</a> now.
			<p/><form method="POST" action="/"><input type="Submit" class="button" value="OK" name="OK"></form>
		</#assign>
		<@getinfodialog title="Update Available" message=message/>
	</#if>
	</div>
</#assign>
<#include "BaseWithNav.ftl" />
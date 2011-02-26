<#include "Shortcuts.ftl">
<#include "GetURLFunction.ftl" />
<#include "GetDialog.ftl" />

<#assign content>

    <div><span class="Text_1">ThreatFactor NSIA</span>
    <br><span class="LightText">Copyright &copy; 2005-2011 ThreatFactor.com, All rights reserved</span>
    <#if version??><br><span class="LightText">Version ${version?html}<#if build_number??> (build ${build_number?html})</#if></span></#if>
    <p>
	<ul>
		<li><a href="http://threatfactor.com/Products/NSIA" target="_blank">View Product Overview</a></li>
		<li><a href="http://lukemurphey.net/projects/nsia/issues/new" target="_blank">File a Bug</a></li>
		<li><a href="http://lukemurphey.net/projects/nsia/wiki" target="_blank">Product Documentation</a></li>
		<li><a href="http://sourceforge.net/projects/nsia/" target="_blank">Get the Source Code</a></li>
		<li><a href="http://www.gnu.org/licenses/agpl-3.0.html" target="_blank">View the License (Affero GPL v3)</a></li>
	</ul>
    
	<form target="_blank" action="https://www.paypal.com/cgi-bin/webscr" method="post">
		<input type="hidden" name="cmd" value="_s-xclick">
		<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----">
		<input type="image" src="https://www.paypal.com/en_US/i/btn/btn_donate_SM.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
		<img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1">
	</form>
</#assign>
<#include "BaseWithNav.ftl" />
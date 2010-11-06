<#include "Shortcuts.ftl">
<#include "GetURLFunction.ftl" />
<#include "GetDialog.ftl" />

<#assign content>

    <div><span class="Text_1">ThreatFactor NSIA</span>
    <br><span class="LightText">Copyright &copy; 2010 ThreatFactor.com, All rights reserved</span>
    <#if version??><br><span class="LightText">Version ${version?html}<#if build_number??> (build ${build_number?html})</#if></span></#if>
    <p>
    <ul>
        <li><a href="http://threatfactor.com/Products/NSIA" target="_blank">Product Overview</a></li>
        <li><a href="http://lukemurphey.net/projects/nsia/issues/new" target="_blank">File a Bug</a></li>
        <li><a href="http://lukemurphey.net/projects/nsia/wiki" target="_blank">Wiki</a></li>
        <li><a href="http://sourceforge.net/projects/nsia/" target="_blank">Source Code</a></li>
        <li><a href="http://www.gnu.org/licenses/agpl-3.0.html" target="_blank">License (Affero GPL v3)</a></li>
    </ul>
</#assign>
<#include "BaseWithNav.ftl" />
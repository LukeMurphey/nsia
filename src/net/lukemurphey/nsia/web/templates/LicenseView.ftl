<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
    <p/><div style="width: 600px">
    
    <#assign keyform>
        <p/>Once you have a valid license key, enter it below:<form method="POST" action="<@url name="license" />">
        <input class="textInput" size="48" type="text" name="LicenseKey" value="${license_key}">
        <input type="Submit" class="button" value="Apply" name="Apply"></form>
    </#assign>
    
    <#if !license?? || license.status = UNLICENSED>
        <#assign message>
            The application does not possess a valid license. Thus, the application cannot receive definition updates. Purchase a license at <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.${keyform}
        </#assign>
        <@getwarndialog title="No License" message=message />
    <#elseif !license_check_completed>
        <#assign message>
            The application license is currently being verified by the application.<p/>If you need support, please go to <a href="http://ThreatFactor.com/Support">ThreatFactor.com</a> now.<p/>
            <form method="POST" action="${request.thisURL}"><input type="Submit" class="button" value="OK" name="OK"></form>
        </#assign>
        <@getinfodialog title="License Being Verified" message=message />
    <#elseif license.status = EXPIRED>
        <#assign message>
            The license has expired. Thus, the application cannot receive definition updates. Purchase an updated license at <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.${keyform}
        </#assign>
            
        <@getwarndialog title="Expired License" message=message />
    <#elseif license.status = ACTIVE>
        <#assign message>
            The application license has been granted to ${license.licensee} and is valid until ${license.expirationDate?date}.<p/>If you need support, please go to <a href="http://ThreatFactor.com/Support/">ThreatFactor.com</a> now.<p/>
            <form method="GET" action="<@url name="main_dashboard" />"><input type="Submit" class="button" value="OK" name="OK"></form>
        </#assign>
            
        <@getinfodialog title="License Up to Date" message=message />
    <#elseif license.status = ILLEGAL>
        <#assign message>
            The license has expired. Thus, the application cannot receive definition updates. Purchase an updated license at <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.${keyform}
        </#assign>
            
        <@getwarndialog title="Illegal License" message=message />
    <#elseif (license.key?? && license.status = UNLICENSED) >
        <#assign message>
            The provided license key is invalid. Go to the <a href="http://ThreatFactor.com/Support/">ThreatFactor support website</a> and check to make sure you entered it correctly if you know you have a valid license. If you need to purchase a license go to <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.${keyform}
        </#assign>
            
        <@getwarndialog title="License Invalid" message=message />
    <#elseif (!license.key?? || license.status = UNVALIDATED) >
        <#assign message>
            The license could not be validated with ThreatFactor.com. This is likely due to a problem with the network connection. Please make sure NSIA can connect to ThreatFactor.com, otherwise, NSIA will not be able to download updated definitions.
            <p/>NSIA will attempt to re-validate the license periodically and will validate the license as soon as it can establish a connection to ThreatFactor.com.
            <p/>If you need additional assistance, go to <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.
        </#assign>
            
        <@getwarndialog title="License Not Yet Validated" message=message />
    <#else>
        <#assign message>            
            NSIA does not have a valid license. Thus, the application cannot receive definition updates. Purchase an updated license at <a href="http://ThreatFactor.com/">ThreatFactor.com now</a>.${keyform}
        </#assign>
            
        <@getwarndialog title="No License" message=message />
    </#if>
    
</#assign>
<#include "BaseWithNav.ftl" />

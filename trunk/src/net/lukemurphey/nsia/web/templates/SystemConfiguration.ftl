<#assign content>
<span class="Text_1">System Configuration</span>
<p>

<#include "GetURLFunction.ftl">
<#include "Shortcuts.ftl">

<#macro config_options options name extra="" extra_at_bottom=""> 
    <tr>
        <td><span style="margin-right: 8px;" class="Text_2">${name?html}</span><#if extra?trim != "">&nbsp;${extra}</#if></td>
    </tr>
    <#list options as param>

    <#assign start_link>
    <a name="${param.id?html}" href="${geturl("system_configuration")}?ParamID=${param.id?html}#${param.id?html}">
    </#assign>

    <#if (selected?? && selected == param.id)>
    <tr class="Background2">
        <td>
            <table width="100%">
                <tr>
                    <td width="1%">${start_link}<img alt="-" src="/media/img/9_TreeNodeOpen"></a></td>
                    <td>${start_link}${param.name?html}</a></td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td colspan="2">
                        <form method="post" action="/System/Configuration">
                        <input type="hidden" name="ParamID" value="${param.id?html}">
               <#if param.integer>
                        <input type="hidden" name="ParamSet" value="true">
                        <input class="textInput" size="48" name="ParamValue" value="${param.value?html}">
               <#elseif param.text>
                        <input type="hidden" name="ParamSet" value="true">
                        <input class="textInput" size="48" name="ParamValue" value="${param.value?html}">
               <#elseif param.bool>
                        <input type="hidden" name="ParamSet" value="true">
                        <table>
                            <tr>
                                <td><input type="CheckBox" name="ParamValue" <#if param.value == "true">checked</#if>>Enabled&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                                <td><input class="button" type="submit" value="Apply"></td>
                            </tr>
                        </table>
               <#elseif param.multiline>
                        <textarea cols="48" rows="5" name="ParamValue">${param.value?html}</textarea>
               <#elseif param.password>
                        <input type="password" class="textInput" size="48" name="ParamValue" value="${param.value?html}">
               <#elseif param.select>
                        <select name="ParamValue">
                            <#if param.selectValues??>
                            <#list param.selectValues as v>
                                <option value="${v.name}" <#if v.name == param.value>selected</#if>>${v.value?html}</option>
                            </#list>
                            </#if>
                        </select>
               </#if>
                                &nbsp;&nbsp;
                                <#if param.bool == false><input class="button" type="submit" value="Apply"></#if>
                            </form>
                        </td>
                   </tr>
               </table>
          </td>
    </tr>
    <#else>
    <tr class="Background1">
        <td>
            <table>
                <tr>
                    <td>${start_link}<img alt="-" src="/media/img/9_TreeNodeClosed"></a></td><td>${start_link}${param.name?html}</a></td>
                </tr>
            </table>
        </td>
    </tr>
    </#if>
</#list>
<#if extra_at_bottom?trim != ""><tr><td style="height:28px;">${extra_at_bottom}</td></tr></#if>
<tr><td>&nbsp;</td></tr>
</#macro>
<#macro extra_message message><span style="background-image: url('/media/img/16_Information'); background-repeat: no-repeat; padding-left: 18px;" class="InfoText">${message}</#macro>

<table width="100%">
    <@config_options options=authentication_options name="Authentication System" />
    <@config_options options=session_options name="Session Management Subsystem" />
    <@config_options options=server_options name="Server Subsystem" />
    <@config_options options=logging_options name="Logging Subsystem" />
    <@config_options options=license_options name="License" />
        
    <#assign email_info>
    <#if !email_from_address?? && !smtp_server??><@extra_message message="You must define at least an SMTP server and a source email address to complete the email setup" /></#if>
    <#if email_from_address?? && !smtp_server??><@extra_message message="You must define an SMTP server to complete the email setup" /></#if>
    <#if !email_from_address?? && smtp_server??><@extra_message message="You must define a source email address to complete the email setup" /></#if>
    <#if email_from_address?? && smtp_server?? && !user_email??><#assign message>You do not have an email address defined in your user profile. Define an email address for your account in order to send a test email. <a href="${geturl("user_editor", "Edit", session.userId)}">[Edit profile now]</a></#assign><@extra_message message=message /></#if>
    </#assign>
    <#assign test_email_link>
    <#if email_from_address?? && smtp_server?? && user_email??><a href="${request.thisURL?html}?SendTestEmail">Send a test email</a> to ${user_email?html}</#if>
    </#assign>
    <@config_options options=email_options name="Email" extra=email_info extra_at_bottom=test_email_link/>
    <@config_options options=scanner_options name="Scanner" />
</table>
</#assign>
<#include "BaseWithNav.ftl">
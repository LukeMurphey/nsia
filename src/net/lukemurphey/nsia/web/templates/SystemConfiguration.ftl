<#assign content>
<span class="Text_1">System Configuration</span>
<p>

<#include "GetURLFunction.ftl">

<#macro config_options options name> 
    <tr>
        <td class="Text_2">${name}</td>
    </tr>
    <#list options as param>

    <#assign start_link>
    <a name="${param.id}" href="${geturl("system_configuration")}?ParamID=${param.id}#${param.id}">
    </#assign>

    <#if (selected?? && selected == param.id)>
    <tr class="Background2">
        <td>
            <table width="100%">
                <tr>
                    <td width="1%">${start_link}<img alt="-" src="/media/img/9_TreeNodeOpen"></a></td>
                    <td>${start_link}${param.name}</a></td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td colspan="2">
                        <form method="post" action="/System/Configuration">
                        <input type="hidden" name="ParamID" value="${param.id}">
               <#if param.integer>
                        <input type="hidden" name="ParamSet" value="true">
                        <input class="textInput" size="48" name="ParamValue" value="${param.value}">
               <#elseif param.text>
                        <input type="hidden" name="ParamSet" value="true">
                        <input class="textInput" size="48" name="ParamValue" value="${param.value}">
               <#elseif param.bool>
                        <input type="hidden" name="ParamSet" value="true">
                        <table>
                            <tr>
                                <td><input type="CheckBox" name="ParamValue" <#if param.value == "true">checked</#if>>Enabled&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                                <td><input class="button" type="submit" value="Apply"></td>
                            </tr>
                        </table>
               <#elseif param.multiline>
                        <textarea cols="48" rows="5" name="ParamValue">${param.value}</textarea>
               <#elseif param.password>
                        <input type="password" class="textInput" size="48" name="ParamValue" value="${param.value}">
               <#elseif param.select>
                        <select name="ParamValue">
                            <#if param.selectValues??>
                            <#list param.selectValues as v>
                                <option value="${v.name}" <#if v.name == param.value>selected</#if>>${v.value}</option>
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
                    <td>${start_link}<img alt="-" src="/media/img/9_TreeNodeClosed"></a></td><td>${start_link}${param.name}</a></td>
                </tr>
            </table>
        </td>
    </tr>
    </#if>
</#list>
<tr><td>&nbsp;</td></tr>
</#macro>

<table width="100%">
    <@config_options options=authentication_options name="Authentication System" />
    <@config_options options=session_options name="Session Management Subsystem" />
    <@config_options options=server_options name="Server Subsystem" />
    <@config_options options=logging_options name="Logging Subsystem" />
    <@config_options options=license_options name="License" />
    <@config_options options=email_options name="Email" />
</table>
</#assign>
<#include "BaseWithNav.ftl">
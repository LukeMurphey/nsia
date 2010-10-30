<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">

<#macro getref reference name>
    <a target="_blank" href="${reference.type.urlPrefix}${reference.value}"> ${name?html} ${reference.value?html}</a><br/>
</#macro>

<#macro ruleselectrow name description typearg >
    <div style="padding:5px;">
    <div class="ToolIcon" style="float:left;"><a href="<@url name="definition" />?Type=${typearg?html}"<img src="/media/img/32_Add"></a></div>
        
    <div style="position:relative; left:8px;"><a href="<@url name="definition" />?Type=${typearg?html}"<span class="Text_2">${name?html}</span></a><br>${description?html}</div>
    <br></div>
</#macro>

<#assign content>
<#include "PopupDialog.ftl">
<#if definition??>
<div style="position:relative;">    
    <div>
        <div style="position: relative; left: 0px;">
            <table cellpadding="2">
                <tr>
                    <td class="Text_2">Name</td>
                    <td>&nbsp;</td><td>${definition.fullName?html}</td>
                </tr>
                <tr>
                    <td class="Text_2">Message</td>
                    <td>&nbsp;</td><td>${definition.message?html}</td>
                </tr>
                <tr>
                    <td class="Text_2">Severity</td><td>&nbsp;</td>
                    <td>${definition.severity?lower_case?cap_first?html}</td>
                </tr>
                <tr>
                    <td class="Text_2">Version</td>
                    <td>&nbsp;</td><td>${definition.revision?html}</td>
                </tr>
                <#if (definition.references?size > 0)>
                <tr>
                    <td class="Text_2">References</td>
                    <td>&nbsp;</td>
                    <td>   
                        <table>
                            <#if definition.official>
                            <tr>
                                <td><img style="margin: 0px" src="/media/img/16_Bullet_arrow"></td>
                                <td><a target="_blank" href="http://ThreatFactor.com/Products/NSIA/DefinitionReference/${definition.ID}">ThreatFactor.com Definition Writeup: ${definition.ID}</a><br/></td>
                           </tr>
                            </#if>
                            <#list definition.references as reference>
                            <tr>
                                <td><img style="margin: 0px" src="/media/img/16_Bullet_arrow"></td>
                                <td>
                                    <#if reference.type = ARACHNIDS >
                                        <@getref reference=reference name="Arachnids" />
                                    <#elseif reference.type = BUGTRAQ >
                                        <@getref reference=reference name="BID" />
                                    <#elseif reference.type = CVE >
                                        <@getref reference=reference name="CVE" />
                                    <#elseif reference.type = MCAFEE >
                                        <@getref reference=reference name="McAfee" />
                                    <#elseif reference.type = NESSUS >
                                        <@getref reference=reference name="Nessus" />
                                    <#elseif reference.type = URL >
                                        <@getref reference=reference name="" />
                                    </#if>
                                </td>
                           </tr>
                           </#list>
                       </table>
                    </td>
                </tr>
                </#if>
            </table>
            <p/>
        </div>
    </div>
</div>
</#if>

<#if (definition??)>
    <#if (!type?? && definition.type = "ThreatPattern")>
        <#if (!code??)>
            <#assign code=definition.ruleCode />
        </#if>
        <#assign type="ThreatPattern" />
    <#elseif (!type??)>
        <#if (!code??)>
            <#assign code=definition.script />
        </#if>
        <#assign type="ThreatScript" />
    </#if>
    
    <#if (!code?? && type = "ThreatPattern")>
        <#assign code=definition.ruleCode />
    <#elseif (!code?? && type = "ThreatScript")>
        <#assign code=definition.script />
    </#if>
    
</#if>

<span class="Text_2">${type?html} Definition</span><p>

<script src="/media/misc/codepress/codepress.js" type="text/javascript"></script>
<script type="text/javascript">
    function submitEditorForm(editorform){
        document.editorform.SignatureCode2.value = cp1.getCode();
        document.editorform.submit();
        return true;
    }
</script>
    
<form name="editorform" id="editorform" action="<#if (definition??)><@url name="definition" args=[definition.ID] /><#else><@url name="definition" args=["New"] /></#if>" method="post" onSubmit="return submitEditorForm(this.form)">
    <textarea id="cp1" class="codepress <#if type = "ThreatPattern">threatpattern<#else>javascript</#if><#if (definition?? && definition.official)> readonly-on</#if>" wrap="off" rows="18" cols="90" name="SignatureCode">${code?html}</textarea>
    <#if (definition?? && definition.ID > -1)>
    <input type="hidden" name="Action" value="Edit">
    <input type="hidden" name="ID" value="${definition.ID?c}">
    <input type="hidden" name="LocalID" value="${definition.localID?c}">
    <#else>
    <input type="hidden" name="Action" value="New">
    </#if>
    <input type="hidden" name="Type" value="${type?html}">    
    <input type="hidden" name="SignatureCode2" value="${code?html}">
            
    <#if definition?? && definition.official >
        <br><input class="button" type="submit" name="Cancel" value="Close">
    <#else>
        <br><input class="button" type="submit" name="Compile" value="Compile">
        <input class="button" type="submit" name="Cancel" value="Cancel">
    </#if>
</form>
<p/>
</#assign>
<#include "BaseWithNav.ftl">
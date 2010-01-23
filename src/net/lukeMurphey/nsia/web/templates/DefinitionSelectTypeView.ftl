<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#macro ruleselectrow name description typearg >
    <div style="padding:5px;">
    <div class="ToolIcon" style="float:left;"><a href="<@url name="definition" args=["New"] />?Type=${typearg?html}"<img src="/media/img/32_Add"></a></div>
        
    <div style="position:relative; left:8px;"><a href="<@url name="definition" args=["New"] />?Type=${typearg?html}"<span class="Text_2">${name?html}</span></a><br>${description?html}</div>
    <br></div>
</#macro>

<#assign content>
<div style="padding:5px;"><span class="Text_2">Select Type of Rule To Add<span></div>
<@ruleselectrow name="ThreatPattern Definition" description="Patterns have a simple syntax and are useful for basic rules. Most issues can be detected with ThreatPatterns." typearg="ThreatPattern" />
<@ruleselectrow name="ThreatScript Definition" description="Executable code can be written in the ThreatScript language to design an analysis engine capable of advanced analysis." typearg="ThreatScript" />
</#assign>

<#include "BaseWithNav.ftl">
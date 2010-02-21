
<div style="padding:5px;"><span class="Text_2">Select Type of Rule To Add<span></div>
<#macro definitionselect name description link >
    <div style="padding:5px;">
        <div class="ToolIcon" style="float:left;">
            <a href="${link?html}"><img src="/media/img/32_Add"></a>
        </div>
                
        <div style="position:relative; left:8px;">
            <a href="${link?html}">
                <span class="Text_2">${name?html}</span>
            </a>
            <br />${description?html}
        </div>
        <br />
    </div>
</#macro>

<@definitionselect name="ThreatPattern Definition" description="Patterns have a simple syntax and are useful for basic rules. Most issues can be detected with ThreatPatterns." link="Definitions?Action=New&Type=Pattern" />
<@definitionselect name="ThreatScript Definition" description="Executable code can be written in the ThreatScript language to design an analysis engine capable of advanced analysis." link="Definitions?Action=New&Type=Script"/>

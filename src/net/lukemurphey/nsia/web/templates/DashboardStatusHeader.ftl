<table class="PanelEntry">
<#if scanner.statusGreen>
    <tr><td><img src="/media/img/16_LEDgreen" alt="Green"></td><td nowrap>${scanner.statusDescription?html}</td></tr>
<#elseif scanner.statusRed>
    <tr><td><img src="/media/img/16_LEDred" alt="Red"></td><td nowrap>${scanner.statusDescription?html}</td></tr>
<#else>
    <tr><td><img src="/media/img/16_LEDyellow" alt="Yellow"></td><td nowrap>${scanner.statusDescription?html}</td></tr>
</#if>
    
<#if manager.statusGreen>
    <tr><td><img src="/media/img/16_LEDgreen" alt="Green"></td><td nowrap>${manager.statusDescription?html}</td></tr>
<#elseif manager.statusRed>
    <tr><td><img src="/media/img/16_LEDred" alt="Red"></td><td nowrap>${manager.statusDescription?html}</td></tr>
<#else>
    <tr><td><img src="/media/img/16_LEDyellow" alt="Yellow"></td><td nowrap>${manager.statusDescription?html}</td></tr>
</#if>
</table>
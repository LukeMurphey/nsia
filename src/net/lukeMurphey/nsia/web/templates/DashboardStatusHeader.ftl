<table class="PanelEntry">
<#if scanner.statusGreen>
    <tr><td><img src="/media/img/16_LEDgreen" alt="Green"></td><td nowrap>${scanner.statusDescription}</td></tr>
<#elseif scanner.statusRed>
    <tr><td><img src="/media/img/16_LEDred" alt="Red"></td><td nowrap>${scanner.statusDescription}</td></tr>
<#else>
    <tr><td><img src="/media/img/16_LEDyellow" alt="Yellow"></td><td nowrap>${scanner.statusDescription}</td></tr>
</#if>
    
<#if manager.statusGreen>
    <tr><td><img src="/media/img/16_LEDgreen" alt="Green"></td><td nowrap>${manager.statusDescription}</td></tr>
<#elseif manager.statusRed>
    <tr><td><img src="/media/img/16_LEDred" alt="Red"></td><td nowrap>${manager.statusDescription}</td></tr>
<#else>
    <tr><td><img src="/media/img/16_LEDyellow" alt="Yellow"></td><td nowrap>${manager.statusDescription}</td></tr>
</#if>
</table>
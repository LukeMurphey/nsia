<#macro getinfodialog title message>
    <@dialog title=title message=message type="Info" />
</#macro>

<#macro getwarndialog title message>
    <@dialog title=title message=message type="Warn" />
</#macro>

<#macro getcritialdialog title message>
    <@dialog title=title message=message type="Crit" />
</#macro>
<#macro getFormErrors form_errors>
    <#if (form_errors??)>
    <div class="FormError">Please correct the errors below and try again</div>
    <ul class="FormErrorList"><#list form_errors.values() as error>
        <#if error.getMessage()??><li><div class="RedText">${error.getMessage()}</div></li></#if>
    </#list></ul>
    </#if>
</#macro>
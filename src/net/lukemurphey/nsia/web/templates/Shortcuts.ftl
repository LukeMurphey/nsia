<#macro iconlink url imageURL imageAlt text>
<table><tr><td><a href="${url}"><img class="imagebutton" src="${imageURL}" alt="${imageAlt}"></a></td><td><a href="${url}">${text}</a></td></tr></table>
</#macro>
<#macro endline>

</#macro>
<#macro infonote message >
    <table><tr><td style="vertical-align: top;"><img src="/media/img/16_Information" alt="Info"/></td>
    <td class="InfoText">${message}<td></tr></table>
</#macro>
<#macro warningnote message >
    <table><tr><td><img src="/media/img/16_Warning" alt="Warning"></td>
    <td class="WarnText">${message}<td></tr></table>
</#macro>
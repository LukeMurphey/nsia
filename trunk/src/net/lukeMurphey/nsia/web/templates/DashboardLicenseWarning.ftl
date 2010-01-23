<#if !license_check_completed>
<td><table>
    <tr>
        <td colspan="2"><span class="RedText">License not validated</span></td></tr>
<#else>
        <td><table><tr><td colspan="2"><span class="RedText">License invalid</span></td></tr>
</#if>

    <#if !license??>
    <#elseif !license_check_completed>
    <tr>
        <td><img src="/media/img/16_Warning"></td><td>License could not be validated <a href="/License">[Details]</a></td>
    </tr>
    <#elseif license.status = EXPIRED>
    <tr>
        <td><img src="/media/img/16_Warning"></td>
        <td>The application license has expired <a href="/License">[Fix Now]</a></td>
    </tr>
    <#elseif (!license.key?? || license.status = UNVALIDATED) >

    <#else>
    <tr>
        <td><img src="/media/img/16_Warning"></td>
        <td>The application is unlicensed <a href="/License">[Fix Now]</a></td></tr>
    </#if>

</td></tr></table>
<#assign content>
    <p>&nbsp;
    <form action="${request.thisURL}" onsubmit="showHourglass('Authenticating...')" method="post">
        <input type="hidden" name="ForwardTo" value="<#if request.getParameter("ForwardTo")??>${request.getParameter("ForwardTo")?html}<#else>${request.thisURL}</#if>" />
        <table align="center">
            <tr>
                <td colspan="99">&nbsp;</td>
            </tr>
            <tr>
                <td class="<#if message?? && (message.severity == information || message.severity == success)>InfoText<#else>WarnText</#if>" width="300px" colspan="99">
                    <#if message??>
                        <table>
                            <tr>
                                <td>
                                    <#if (message.severity == information)><img src="/media/img/16_Information" /></#if>
                                    <#if (message.severity == alert)><img src="/media/img/16_Alert" /></#if>
                                    <#if (message.severity == success)><img src="/media/img/16_Information" /></#if>
                                    <#if (message.severity == warning)><img src="/media/img/16_Warning" /></#if>
                                </td>
                                <td>${message?html}</td>
                            </tr>
                        </table>
                    </#if></td>
            </tr>
            <tr>
                <td class="Text_2">Login:</td>
                <td><input class="textInput" size="30" name="Username" type="text"></td>
            </tr>
            <tr>
                <td class="Text_2">Password:</td>
                <td><input class="textInput" size="30" name="Password" type="password"></td>
            </tr>
            <tr>
                <td align="right" colspan="2"><input class="button" type="submit" value="Login"><input type="hidden" value="null" name="BannerCheck"></td>
            </tr>
            <tr>
                <td colspan="99">&nbsp;</td></tr>
         </table>
     </form>
</#assign>
<#include "Base.ftl">
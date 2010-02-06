<table border="0" cellpadding="0" cellspacing="8">
    <tr>
        <td class="AlignedTop" rowspan="2"><img src="${icon}" alt="Warning" />&nbsp;&nbsp;</td>
        <td class="Text_2"><#if warn??><span class="Text_1"><span class="WarnText">${title}</span></span><#else><span class="Text_1">${title}</span></#if><td>
    </tr>
    <tr>
        <td>${message}<td>
    </tr>
    <#if (suggested??)>
    <tr>
        <td>&nbsp;</td>
        <td>&nbsp;<td>
    </tr>
    <tr>
        <td>&nbsp;</td>
        <td><a href="${suggested.link}">[${suggested.title}]</a><td>
    </tr>
    </#if>
    <#if progress??>
    <#if ( progress >= 0 )>
    <tr>
        <td>&nbsp;</td>
        <#function width progress>
            <#return 363 * progress * 0.01>
        </#function>
        <td>
            <div style="position:relative; width:370px; height:20px; padding:5px; background-image:url(/media/img/ProgressBarBlank); background-repeat: no-repeat;">
                <div id="ProgressBar" style="top:4; left:4; width:${width(progress)}px; height:9px; padding:5px; background-image:url(/media/img/ProgressBar); layer-background-image:url(/media/img/ProgressBar); background-repeat: repeat-x;"></div>
            </div>
        </td>
    </tr>
    <#else>
    <tr>
        <td>&nbsp;</td>
        <td height="34" valign="top"><img alt="Working..." src="/media/img/ProgressBarAnimation"></td>
    </tr>
    </#if>
    </#if>
    <#if buttons??>
    <tr>
        <td>&nbsp;</td>
        <td>
        <#list buttons as button>
            <span style="float:left">
                <form action="${button.link}" method="GET">
                    <input class="button" type="submit" value="${button.title}" name="Selected" />
                </form>
            </span>&nbsp;
        </#list>
        </td>
    </tr>
    </#if>
</table>
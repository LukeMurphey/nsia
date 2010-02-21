<td><script language="JavaScript" type="text/javascript">
if (document.all || document.getElementById)
    startit();
else
    window.onload = startit;
</script>
    <table>
        <tr>
            <td><span class="Text_3">Automatic Update</span></td>
            <td rowspan="200">
                <form method="get" action="${refresh_url}" name="reloadForm">        
                    <select name="refreshRate" onchange='onRateChange(this)'>
                    <#if refresh_rate = "Disable">
                        <script language="JavaScript">setRefreshRate('Disable'); resetCountDown();</script>
                    <#else>
                        <script language="JavaScript">setRefreshRate("${refresh_rate?html}"); resetCountDown();</script>
                    </#if>
                        <option value="15" <#if refresh_rate = "15">selected</#if>>15 seconds</option>
                        <option value="30" <#if refresh_rate = "30">selected</#if>>30 seconds</option>
                        <option value="60" <#if refresh_rate = "60">selected</#if>>1 minute</option>
                        <option value="120" <#if refresh_rate = "120">selected</#if>>2 minutes</option>
                        <option value="300" <#if refresh_rate = "300">selected</#if>>5 minutes</option>
                        <option value="600" <#if refresh_rate = "600">selected</#if>>10 minutes</option>
                        <option value="900" <#if refresh_rate = "900">selected</#if>>15 minutes</option>
                        <option value="Disable" <#if refresh_rate = "Disable">selected</#if>>Disable</option>
                    </select>
                </form>
           </td>
        <tr>
            <td width="200"><span style="cursor: hand;" onclick="onPlayPauseClick()">
            <img style="display: inline" id="refresh_pause" src="/media/img/16_Pause"><img style="display: none" id="refresh_play" src="/media/img/16_Play"></span>
            &nbsp;Refresh <span id="countDownText">---</span>
            
            </td>
        </tr>
    </table>
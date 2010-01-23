<#if tasks?size == 1>
<table>
    <tr>
        <td><span class="Text_3">${tasks[0].workerThread.taskDescription} </span></td>
    </tr>
    <tr>
        <td><@truncate_chars length=32>${tasks[0].workerThread.statusDescription}</@truncate_chars><a href="/Tasks">[View]</a></td>
    </tr>
</table>
<#elseif (tasks?size > 1) >
<table>
    <tr>
        <td><span class="Text_3">${tasks?size} Tasks Running</span></td>
    </tr>
    <tr>
        <td>Multiple background tasks are running <a href="/Tasks">[View]</a></td>
    </tr>
</table>
</#if>

<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#function width progress>
    <#return 194 * progress * 0.01>
</#function>
<#assign content>

<#if (tasks?size > 0)>
<#include "PopupDialog.ftl">
<div class="SectionHeader">Background Tasks</div>
<p>
<table class="DataTable">
    <thead>
        <tr>
            <td>Title</td>
            <td>User</td>
            <td>Description</td>
            <td colspan="2">Progress</td>
        </tr>
    </thead>
    <tbody>
    <#list tasks as task>
        <tr>
            <td class="TitleText">${task.taskDescription?html}</td>
            <td>
                <#if task.user?? >
                <table>
                    <tr>
                        <td><img style="vertical-align: top;" alt="User" src="/media/img/<#if task.user.unrestricted>16_Admin<#else>16_User</#if>">
                        <td>&nbsp;<a href="<@url name="user" args=[task.user.userID]/>">${task.user.userName?html}</a></td>
                    </tr>
                </table>
                <#else>
                <table>
                    <tr>
                        <td><img style="vertical-align: top;" alt="System" src="/media/img/16_System">&nbsp;System</td>
                    </tr>
                </table>
                </#if>
            </td>
            <td>${task.statusDescription?html}</td>
            <td>
            <#if (task.progress < 0)>
                <img style="margin-top: 3px;" src="/media/img/SmallProgressBarAnimation" alt="Progress Bar" />
            <#else>
                <div style="position:relative; width:198px; height:12px; margin-top: 3px; padding:2px; background-image:url(/media/img/SmallProgressBarBlank);">
                    <div style="position:relative; left:1px; width:${width(task.progress)?c}px; height:8px; padding:2px; background-image:url(/media/img/SmallProgressBar2); layer-background-image:url(/media/img/SmallProgressBar2);"></div>
                </div>
            </#if>
            </td>
            <td>
                <table>
                    <tr>
                        <td><a class="taskstop" href="<@url name="task_stop" args=[task.uniqueName] />">&nbsp;<img class="imagebutton" src="/media/img/16_Delete"></a></td>
                        <td><a class="taskstop" href="<@url name="task_stop" args=[task.uniqueName] />">Stop</a></td>
                    </tr>
                </table>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
<script type="text/javascript">
    $(document).ready(
        function(){
            $('a.taskstop').click(
                function(){
                    openDeleteConfirmDialog( "Are you sure you want to stop this task? ", "Stop Task?", this.href );
                    return false;
                }
            );
        }
    );
</script>
<#else>
    <p />
    <@getinfodialog title="No Tasks Running" message="No tasks are currently running." />
</#if>
</#assign>
<#include "BaseWithNav.ftl">
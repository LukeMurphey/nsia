<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#function width progress>
    <#return 194 * progress * 0.01>
</#function>
<#assign content>
<div class="SectionHeader">Background Tasks</div>
<#if (tasks?size > 0)>
<p>
<table class="DataTable">
    <thead>
        <tr>
            <td>Title</td>
            <td>User</td>
            <td>Description</td>
            <td>Progress</td>
            <td>&nbsp;</td>
        </tr>
    </thead>
    <tbody>
    <#list tasks as task>
        <tr>
            <td class="TitleText">${task.taskDescription}</td>
            <td>
                <#if task.user?? >
                <table>
                    <tr>
                        <td><img style="vertical-align: top;" alt="User" src="/media/img/<#if task.user.unrestricted>16_Admin<#else>16_User</#if>">
                        <td>&nbsp;<a href="UserManagement?UserID=${task.user.id}">${task.user.username}</a></td>
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
            <td>${task.statusDescription}</td>
            <td>
            <#if (task.progress < 0)>
                <img style="margin-top: 3px;" src="/media/img/SmallProgressBarAnimation" alt="Progress Bar" />
            <#else>
                <div style="position:relative; width:198px; height:12px; margin-top: 3px; padding:2px; background-image:url(/media/img/SmallProgressBarBlank);">
                    <div style="position:relative; left:1px; width:${width(task.progress)}px; height:8px; padding:2px; background-image:url(/media/img/SmallProgressBar2); layer-background-image:url(/media/img/SmallProgressBar2);"></div>
                </div>
            </#if>
            </td>
            <td>
                <table>
                    <tr>
                        <td><a href="<@url name="system_task_stop" args=[task.uniqueName] />"&nbsp;<img src="/media/img/16_Delete"></a></td>
                        <td><a href="<@url name="system_task_stop" args=[task.uniqueName] />">Stop</a></td>
                    </tr>
                </table>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
<#else>
    <p />
    <@getinfodialog title="No Tasks Running" message="No tasks are currently running." />
</#if>
</#assign>
<#include "BaseWithNav.ftl">
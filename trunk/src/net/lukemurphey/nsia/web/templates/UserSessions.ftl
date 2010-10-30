<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#macro button url imageURL imageAlt text>
<table><tr><td><a href="${url}"><img class="imagebutton" src="${imageURL}" alt="${imageAlt?html}"></a></td><td><a href="${url}">${text?html}</a></td></tr></table>
</#macro>
<#assign content>
<#include "PopupDialog.ftl">
    <div><span class="Text_1">User Sessions</span>
    <br><span class="LightText">Lists all users currently logged in</span>
    <p>
    <table class="DataTable">
        <thead>
            <tr>
                <td>User ID</td>
                <td >User Name</td>
                <td>Session Assigned</td>
                <td colspan="2">Session Status</td>
            </tr>
        </thead>
        <tbody>
        <#list sessions as session>
            <tr class="Background1">
                <td>${session.userId?c}</td>
                <td><span class="Centered"><a href="<@url name="user" args=[session.userId]/>"><img src="/media/img/16_User"></a><a href="<@url name="user" args=[session.userId]/>">${session.userName?html}</a></span></td>
                <td>${session.sessionCreated}</td>
                <td>${session.sessionStatus.description?html}</td>
                <td class="sessiondelete"><@button url=geturl("user_session_end", session.trackingNumber?c) imageURL="/media/img/16_Delete" imageAlt="Terminate" text="Terminate" /></td>
            </tr>
        </#list>
        </tbody>
    </table>
    <script type="text/javascript">
        $(document).ready(
            function(){
                $('td.sessiondelete a').click(
                    function(){
                        openDeleteConfirmDialog( "Are you sure you want to delete this session?", "Delete Session?", this.href );
                        return false;
                    }
                );
            }
        );
    </script>
</#assign>
<#include "BaseWithNav.ftl">
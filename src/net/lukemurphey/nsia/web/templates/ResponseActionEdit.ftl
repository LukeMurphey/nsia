<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#include "Forms.ftl">
<#include "Shortcuts.ftl">
<#include "FormLayout.ftl">

<#assign content>
	<#assign jquerytools><script type="text/javascript" language="javascript" src="/media/js/jquery.tools.min.js"></script></#assign>
    <#assign extrafooter=[jquerytools] />
    <script type="text/javascript">
		$(document).ready( function(){
			$("td[title]").tooltip({
		
				// place tooltip on the right edge
				position: "center right",
		
				// a little tweaking of the position
				offset: [-2, 10],
		
				// use the built-in fadeIn/fadeOut effect
				effect: "fade",
		
				// custom opacity setting
				opacity: 0.8
		
			}).dynamic({ bottom: { direction: 'down', bounce: true } });
		});
    </script>

    <#if ( hook?? )>
        <div class="Text_2">Edit Response Action</div>Response type: ${hook.action.description?html}<p/>
        <#assign layout=hook.action.layoutWithValues />
    <#else>
        <div class="Text_2">Add a New Response</div>Response type: ${extension.description?html}<p/>
        <#assign layout=extension.fieldLayout />
    </#if>
    <#if (form_errors??)>
    <@getFormErrors form_errors=form_errors />
    </#if>
    <form method="post" action="<#if hook??><@url name="sitegroup_action_editor" args=["Edit", hook.eventLogHookID] /><#else><@url name="sitegroup_action_editor" args=["New"] /></#if>">
        <table class="DataTable" summary="Actions">
            <tbody>
                <@formtable fields=layout request=request />
                <tr class="Background3">
                    <td class="alignRight" colspan="2">
                    <#if ( hook?? )>
                        <input type="hidden" name="Action" value="Edit">
                        <input type="hidden" name="ActionID" value="${hook.eventLogHookID?c}">
                        <input type="submit" class="button" name="Edit" value="Apply Changes">
                    <#else>
                        <input type="hidden" name="Action" value="New">
                        <input type="hidden" name="Extension" value="${extension.name?html}">
                        <input type="submit" class="button" name="Create" value="Create Action">
                        <input type="hidden" name="SiteGroupID" value="${siteGroup.groupId?c}">
                    </#if>
                        <input type="submit" class="button" value="Cancel" name="Cancel">
                    </td>
                </tr>
            </tbody>
        </table>
    </form>
</#assign>
<#include "BaseWithNav.ftl">
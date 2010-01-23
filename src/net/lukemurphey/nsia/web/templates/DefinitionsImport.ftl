<#include "GetURLFunction.ftl">
<#include "GetDialog.ftl">
<#assign content>
    <span class="Text_1">Upload the Definitions File</span>
    <br />Select the definitions file to upload in order to incorporate the definitions into the active set<br>&nbsp;<br>
    <form method="post" enctype="multipart/form-data" action="${request.thisURL}"><input type="file" name="DefinitionsFile" />&nbsp;&nbsp;
    <input type="submit" value="Upload"></form> 
</#assign>
<#include "BaseWithNav.ftl">
<#assign content>
<script type="text/javascript">
    $.ajaxSetup ({
        cache: false
    });

    $(document).ready(function() {
        setInterval(updateProgress, 4 * 1000);
    });
    
    function ajaxcallback(responseText, textStatus, XMLHttpRequest) {
        if( XMLHttpRequest.status != 200 ){
            window.location.href=window.location.href
        }
    }
    
    function updateProgress()
    {
        $("#content").load("${ajaxurl}", {}, ajaxcallback);
    }
</script>
<#if noajaxurl??>
<#assign meta_refresh>
<noscript>
    <meta name='Refresh' http-equiv="Refresh" content="5;URL=${noajaxurl}">
</noscript>
</#assign>
<#assign extrahead=[meta_refresh] />
</#if>
<div style="width: 500px; padding: 64px; padding-left:200px;"><div id="content">${content}</div></div>
</#assign>
<#include "Base.ftl">
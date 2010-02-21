<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
    <#if extrahead??>
        <#list extrahead as head>
            ${head}
        </#list>
    </#if>
    <link rel="shortcut icon" href="/media/img/16_appicon.ico" type="image/x-icon">
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <title>${title?html}</title>
    <style type="text/css">
        @import url('/media/css/Stylesheet.css');
    </style>
    <script type="text/javascript" language="javascript" src="/media/js/RefreshScript.js"></script>
    <script type="text/javascript" language="javascript" src="/media/js/jquery-1.3.2.min.js"></script>
</head>

<body class="ContentMain" face="Arial, sans-serif">
${content}
</body>

</html>
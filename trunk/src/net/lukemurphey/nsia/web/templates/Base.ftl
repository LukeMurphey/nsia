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
    <title>${title}</title>
    <style type="text/css">
        @import url('/media/css/Stylesheet.css');
    </style>
    <script type="text/javascript" language="javascript" src="/media/js/RefreshScript.js"></script>
    <script type="text/javascript" language="javascript" src="/media/js/jquery-1.3.2.min.js"></script>
</head>

<body face="Arial, sans-serif">
    <script type="text/javascript" language="javascript" src="/media/js/MainScript.js"></script>
    <div id="hourglass" class="hourglassInvisible">
        <table summary="StatusWindow">
            <tr>
                <td><img src="/media/img/Loading" alt="loading"></td>
                <td><span id="hourglassText" class="hourglassText">Processing...</span></td>
            </tr>
        </table>
    </div>

    <table class="MainTable" cellpadding="0" cellspacing="0" id="MainTable" summary="Main Page Content" align="center">
    <tr>
        <td class="TitleRow1" width="100%">
            <table summary="HeaderBanner" cellpadding="0" cellspacing="0" class="HeaderImageTable" width="100%">
                <tr>
                    <td rowspan="2"><img class="MainImage" src="/media/img/TitleBanner" alt="NSIA"></td></tr>
                <tr>
                    <td class="AlignedBottom">
                        <table summary="UserOptions" class="Toolbar" valign="bottom" cellspacing="3">
                            <tr><td>&nbsp;</td><td><div style="height: 24px; vertical-align: middle;"><div class="LightText"><#if session?? && session.userName??>Logged in as <u>${session.userName}</u></#if><#if upperbar_options??><#list upperbar_options as option>&nbsp;&nbsp;<a class="LightText" href="${option.link}">${option.title}</a>&nbsp;</#list></#if></div></td></tr>
                        </table>
                    </td></tr>
            </table>
        </td></tr>
    <tr>
        <td class="TopBottomBorder2">
        <table class="DashboardPanel" summary="DashboardPanel">
            <tr><td height="2"></td></tr>
            <tr><td><#if dashboard_headers??>
                <table>
                    <tr>
                        <#list dashboard_headers as header>
                        <td>${header}</td>
                        <#if !show_splitter_border?? || show_splitter_border ><#if (header_index == 0 || header_has_next == true)><td class="PanelSplitter">&nbsp;</td><td>&nbsp;</td></#if></#if></#list>
                    </tr>
                </table></#if>
                </td>
            </tr>    
            <tr><td height="2"></td></tr>
        </table>
        </td></tr>

    <tr class="ContentMain">
        <td>
        ${content}
        </td></tr>

    <tr>
        <td class="Footer" valign="middle" align="right" height="32">
            <span class="Footer">Version <#if version??>${version}</#if>&nbsp;&nbsp;&nbsp;</span></td></tr>
    </table>
    &nbsp;
</body>

</html>
<#if !isajax?? || !isajax><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="/media/img/16_appicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="/media/css/Stylesheet.css" type="text/css" media="all" />
    <link rel="stylesheet" href="/media/css/default-theme/jquery-ui-1.8.5.custom.css" type="text/css" media="all" />
    <title>${title?html}</title>
    <script type="text/javascript" language="javascript" src="/media/js/viewrefresh.js"></script>
    <script type="text/javascript" language="javascript" src="/media/js/jquery-1.4.2.min.js"></script><#-- If you change this reference, then make sure to update Basic.ftl too -->
    <script type="text/javascript" language="javascript" src="/media/js/jquery-ui-1.8.5.custom.min.js"></script>
    
    <#if extrahead??>
        <#list extrahead as head>
${head}
        </#list>
    </#if>
</head>

<body face="Arial, sans-serif">
    <script type="text/javascript" language="javascript" src="/media/js/hourglass.js"></script>
    <div id="hourglass" style="display:none" class="hourglass">
        <table summary="StatusWindow">
            <tr>
                <td><img src="/media/img/LoadingDark" alt="loading"></td>
                <td><span id="hourglassText" class="hourglassText">Processing...</span></td>
            </tr>
        </table>
    </div>

    <table class="MainTable" cellpadding="0" cellspacing="0" id="MainTable" summary="Main Page Content" align="center"></#if>
    <tr>
        <td class="TitleRow1" width="100%">
            <table summary="HeaderBanner" cellpadding="0" cellspacing="0" class="HeaderImageTable" width="100%">
                <tr>
                    <td rowspan="2"><img class="MainImage" src="/media/img/TitleBanner" alt="NSIA"></td></tr>
                <tr>
                    <td class="AlignedBottom">
                        <table summary="UserOptions" class="Toolbar" valign="bottom" cellspacing="3">
                            <tr><td>&nbsp;</td><td><div style="height: 24px; vertical-align: middle;"><div class="LightText"><#if session?? && session.userName?? && context??>Logged in as <u>${session.userName?html}</u></#if><#if upperbar_options??><#list upperbar_options as option>&nbsp;&nbsp;<a class="LightText" href="${option.link}">${option.title?html}</a>&nbsp;</#list></#if></div></td></tr>
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
        </td>
    </tr>
    <tr>
        <td class="Footer" valign="middle" align="right" height="32">
        	<div style="display: none" id="aboutdialog" title="About NSIA">
				<p class="title">ThreatFactor NSIA</p>
				
				Copyright &copy; 2005-2011 ThreatFactor.com, All rights reserved.<br />
				<#if version??>Version ${version?html}<#if build_number??> (build ${build_number?html})</#if></#if>
				
				<ul>
					<li><a href="http://threatfactor.com/Products/NSIA" target="_blank">View Product Overview</a></li>
					<li><a href="http://lukemurphey.net/projects/nsia/issues/new" target="_blank">File a Bug</a></li>
					<li><a href="http://lukemurphey.net/projects/nsia/wiki" target="_blank">Product Documentation</a></li>
					<li><a href="http://sourceforge.net/projects/nsia/" target="_blank">Get the Source Code</a></li>
					<li><a href="http://www.gnu.org/licenses/agpl-3.0.html" target="_blank">View the License (Affero GPL v3)</a></li>
				</ul>
				
				<form target="_blank" action="https://www.paypal.com/cgi-bin/webscr" method="post">
					<input type="hidden" name="cmd" value="_s-xclick">
					<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----">
					<input type="image" src="https://www.paypal.com/en_US/i/btn/btn_donate_SM.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
					<img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1">
				</form>
    	
			</div>
            <span class="Footer"><a id="aboutdialoglink" href="<@url name="about" />">About NSIA</a>&nbsp;&nbsp;&nbsp;</span></td></tr>
            <script type="text/javascript">
				function openAboutDialog(){
					$( "#aboutdialog" ).dialog({
						modal: true,
						width: 400,
						buttons: {
							Ok: function() {
								$( this ).dialog( "close" );
								unpauseTemporarily();
							}
						}
					});
					pauseTemporarily();
					return false;
				}
		      
		        $(document).ready(function() {
		              $('#aboutdialoglink').click( openAboutDialog );
		        });
			</script>
    <#if !isajax?? || !isajax></table>
    &nbsp;
    <#if extrafooter??>
        <#list extrafooter as footer>
${footer}
        </#list>
    </#if>
</body>

</html></#if>
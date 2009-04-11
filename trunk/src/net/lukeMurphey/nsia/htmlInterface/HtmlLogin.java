package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;
import javax.servlet.http.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.SessionStatus;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.trustBoundary.ApiSystem;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlLogin extends HtmlContentProvider {
	
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, IOException{
		return getHtml( requestDescriptor, null );
	}
	
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws IOException{
		
		String title = "Login";
		StringBuffer output = new StringBuffer();
		
		// 1 -- Determine if cookies are supported
		if( requestDescriptor.request.getCookies() == null || requestDescriptor.request.getCookies().length == 0){
			
			// 1.1 -- Display warning if the check failed
			if( requestDescriptor.request.getParameter("CookieCheck") != null ){
				output.append( Html.getDialog( "Cookies are needed to log in but your browser is rejecting them. An HTTP cookie will be used for session management and will not be used to retain passwords or other information that may compromise security. The cookie will be deleted upon closure of the browser or successful logout.<p>Please enable cookies and <a href=\"" + requestDescriptor.request.getRequestURL() + "\">try again.</a><p>", "Cookies are Required", "/32_Warning" ) );
				
				return new ContentDescriptor( "Cookie Support Required", output);
			}
			else{
				requestDescriptor.response.addCookie( new Cookie("BannerCheck", "False") );
				//response.encodeRedirectURL("Console?CookieCheck=1");
				requestDescriptor.response.sendRedirect( requestDescriptor.response.encodeRedirectURL("Console?CookieCheck=1" ) );
				return new ContentDescriptor( "Redirect", "Redirecting...");
			}
		}
		
		// 2 -- Show the banner if necessary
		String loginBannerEx = null;
		ApiSystem system = new ApiSystem( Application.getApplication() );
		
		try {
			loginBannerEx = Html.addBreaks( StringEscapeUtils.escapeHtml(  system.getLoginBanner() ) );
		} catch (GeneralizedException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
		}
		
		boolean cookieBannerCheckSuccess = false;
		
		Cookie[] cookies = requestDescriptor.request.getCookies();
		
		for( int c = 0; c < cookies.length; c++ ){
			if( cookies[c].getName().matches("BannerCheck") && cookies[c].getValue().matches("Accept") )
				cookieBannerCheckSuccess = true;
		}
		
		if(  requestDescriptor.request.getParameter("ShowBanner") != null
				|| cookieBannerCheckSuccess == false
				&& (
						requestDescriptor.request.getParameter("BannerCheck") == null
						|| !requestDescriptor.request.getParameter("BannerCheck").matches("Accept")
						
				) && loginBannerEx != null ){
			output.append("<p>");
			output.append( Html.getDialog( loginBannerEx + "<p><form method=\"post\" action=\"Console\"><input class=\"button\" type=\"submit\" value=\"Accept\" name=\"BannerCheck\"></form><p>", "Use Subject to Monitoring", "/32_Warning", true ) );
			return new ContentDescriptor( "Login Banner", output );
		}
		else{
			requestDescriptor.response.addCookie( new Cookie("BannerCheck", "Accept") );
		}
		
		// 3 -- Output the login dialog
		//output.append( "<div style=\"display: none; position: fixed;\" id=\"hourglass3\" style=\"background-color: white;\"><table><tr><td><!-- <img src=\"/Loading\"> --></td><td><span id=\"hourglassText\" class=\"hourglassText\">Processing...</span></td></tr></table></div>" );
		//output.append( "<div style=\"background-color: white;\" id=\"hourglass2\"><a href=\"\" onclick=\"new Effect.Appear( 'hourglass3' ); return false;\">Tree</a></div>" );
		//output.append( "<p>&nbsp;<form onsubmit=\"new Effect.Appear( 'hourglass' );\" method=\"post\"><table align=\"center\">" );
		
		output.append( "<p>&nbsp;<form action=\"Login\" onsubmit=\"showHourglass('Authenticating...')\" method=\"post\"><table align=\"center\">" );
		output.append( "<tr><td colspan=\"99\">&nbsp;</td></tr>" );
		output.append( "<tr><td width=\"300px\" colspan=\"99\">" );
		
		// Output any relevant messages
		if(  requestDescriptor.authenticationAttempt == WebConsoleServlet.AUTH_LOGOUT_SUCCESS || requestDescriptor.authenticationAttempt == WebConsoleServlet.AUTH_LOGOUT_FAIL ){
			output.append( Html.getInfoNote("You have successsfully logged out") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if ( requestDescriptor.authenticationAttempt == WebConsoleServlet.AUTH_ATTEMPT_FAIL ){
			output.append( Html.getWarningNote("Authentication failed") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if( requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_IDENTIFIER_EXPIRED ) ){
			output.append( Html.getInfoNote("Your session has expired, please re-authenticate") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if ( requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_EXPIRED ) ){
			output.append( Html.getInfoNote("Your session has expired, please re-authenticate") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if ( requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_LIFETIME_EXCEEDED ) ){
			output.append( Html.getInfoNote("The maximum session time has exceeded, please re-authenticate") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if ( requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_INACTIVE ) ){
			output.append( Html.getInfoNote("Your session has expired due to inactivity") );
			requestDescriptor.response.addCookie( new Cookie("SessionID", "") );
		}
		else if ( requestDescriptor.authenticationAttempt == WebConsoleServlet.AUTH_ATTEMPT_NO_PASSWORD ){
			output.append( Html.getWarningNote("Please provide a password to authenticate") );
		}
		else if ( requestDescriptor.authenticationAttempt == WebConsoleServlet.AUTH_ATTEMPT_NO_USERNAME ){
			output.append( Html.getWarningNote("Please provide a username to authenticate") );
		}
		
		
		output.append( "<tr><td class=\"Text_2\">Login:</td><td><input class=\"textInput\" size=\"30\" name=\"Username\" type=\"text\"" );
		
		// Output the username if provided
		if( requestDescriptor.request.getParameter("Username") != null )
			output.append( " value=\"" + StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Username") ) + "\"" );
		
		output.append( "></td></tr>" );
		output.append( "<tr><td class=\"Text_2\">Password:</td><td><input class=\"textInput\" size=\"30\" name=\"Password\" type=\"password\"></td></tr>" );
		output.append( "<tr><td align=\"right\" colspan=\"2\"><input class=\"button\" type=\"submit\" value=\"Login\"><input type=\"hidden\" value=\"" + requestDescriptor.request.getParameter("BannerCheck") + "\" name=\"BannerCheck\"></td></tr>" );
		output.append( "<tr>	<td colspan=\"99\">&nbsp;</td></tr>" );
		output.append( "</table>" );
		
		return new ContentDescriptor( title, output );
	}
}

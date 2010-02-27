package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.Application.ShutdownRequestSource;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ShutdownView extends View {

	public ShutdownView() {
		super("System/Shutdown", "system_shutdown", Pattern.compile("(Confirmed)?", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL() throws URLInvalidException{
		ShutdownView view = new ShutdownView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Make sure the user did not press cancel
		if( request.getParameter("Selected") != null && request.getParameter("Selected").equalsIgnoreCase("Cancel") ){
			response.sendRedirect(MainDashboardView.getURL());
			return true;
		}
		
		// 2 -- Check permissions
		try {
			if( Shortcuts.hasRight(context.getSessionInfo(), "System.Shutdown") == false ){
				context.addMessage("You do not have permission to shutdown the system", MessageSeverity.WARNING);
				response.sendRedirect(SystemStatusView.getURL());
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Add the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 4 -- Render the view
		boolean confirmed = false;
		
		for (String arg : args) {
			if( "Confirmed".equalsIgnoreCase(arg) ){
				confirmed = true;
			}
		}
		
		if( confirmed ){
			// Render the dialog indicating that the manager is shutting down
			Dialog.getDialog(response, context, data, "NSIA is shutting down. The web interface will no longer be available.", "Shutting Down", DialogType.INFORMATION);
			
			// Shutdown the system
			Application.getApplication().shutdown(ShutdownRequestSource.API);
		}
		else{
			// Render the request dialog
			Dialog.getOptionDialog(response, context, data, "Are you sure you want to shutdown the manager?", "System Shutdown", DialogType.INFORMATION, new Link("Shutdown", createURL("Confirmed")), new Link("Cancel", StandardViewList.getURL("main_dashboard")));
		}
		
		return true;
	}

}

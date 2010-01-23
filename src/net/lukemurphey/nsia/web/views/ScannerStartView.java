package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
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

public class ScannerStartView extends View {

	public ScannerStartView() {
		super("Scanner/Start", "scanner_start");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		try {
			Shortcuts.checkRight( context.getSessionInfo(), "System.ControlScanner");
		} catch (InsufficientPermissionException e) {
			Dialog.getDialog(response, context, data, "You do not have permission to start the scanner", "Permission Denied", DialogType.INFORMATION, new Link("Return to Dashboard", StandardViewList.getURL("main_dashboard")));
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		}
		
		Application.getApplication().logEvent( EventLogMessage.Category.SCANNER_STARTED,
				new EventLogField( FieldName.SOURCE_USER_NAME,  context.getSessionInfo().getUserName()),
				new EventLogField( FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() ) );
		
		context.getSessionMessages().addMessage(context.getSessionInfo(), "Scanner was successfully started", MessageSeverity.SUCCESS);
		
		// 1 -- Perform the operation
		Application.getApplication().getScannerController().enableScanning();
		
		response.sendRedirect(StandardViewList.getURL("main_dashboard"));
		
		return true;
	}

}

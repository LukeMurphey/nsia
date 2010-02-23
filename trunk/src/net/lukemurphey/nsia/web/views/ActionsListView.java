package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogHook;
import net.lukemurphey.nsia.eventlog.SiteGroupStatusEventLogHook;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ActionsListView extends View {

	public static final String VIEW_NAME = "sitegroup_actions";
	
	public ActionsListView() {
		super("Actions/SiteGroup", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		ActionsListView actionsList = new ActionsListView();
		return actionsList.createURL(siteGroup.getGroupId());
	}
	
	public static String getURL( long siteGroupID ) throws URLInvalidException{
		ActionsListView actionsList = new ActionsListView();
		return actionsList.createURL(siteGroupID);
	}
	
	private long[] getHooks( HttpServletRequest request){
		Vector<Long> hookIDs = new Vector<Long>();
		
		for( String param : request.getParameterValues("ActionID") ){
			hookIDs.add( Long.valueOf(param));
		}
		
		long[] a = new long[hookIDs.size()];
		
		for( int c =0; c < a.length; c++ ){
			a[c] = hookIDs.get(c);
		}
		
		return a;
	}
	
	private int deleteHooks( RequestContext context, long[] hooks ) throws SQLException, NoDatabaseConnectionException {
		
		// 1 -- Delete the hooks
		int hooksDeleted = 0;
		
		for (long hookID : hooks) {
			EventLogHook.delete(hookID);
			
			Application.getApplication().getEventLog().deleteHook(hookID);
			
			hooksDeleted++;
		}
		
		context.addMessage(hooksDeleted + " actions have been deleted", MessageSeverity.SUCCESS);
		
		return hooksDeleted;
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			// 0 -- Check permissions
			//TODO Check rights
			
			// 1 -- Get the site group
			SiteGroupDescriptor siteGroup = null;
			
			try{
				int siteGroupID = Integer.valueOf(args[0]);
				SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
				siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
			}
			catch( NotFoundException e ){
				Dialog.getDialog(response, context, data, "The Sitegroup associated with the given rule could not be found", "SiteGroup Not Found", DialogType.INFORMATION);
				return true;
			}
			
			data.put("siteGroup", siteGroup);
			
			// 2 -- Perform actions
			if( "Delete".equalsIgnoreCase( request.getParameter("Action") ) ) {
				deleteHooks(context, getHooks(request));
			}
			
			// 3 -- Get the actions
			EventLogHook[] hooks = SiteGroupStatusEventLogHook.getSiteGroupEventLogHooks(Application.getApplication(), siteGroup.getGroupId());
			data.put("actions", hooks);
			
			// 4 -- Get the menu
			data.put("menu", Menu.getSiteGroupMenu(context, siteGroup));
			
			// 5 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add( new Link("Main Dashboard", MainDashboardView.getURL()) );
			breadcrumbs.add( new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroup.getGroupId())) );
			breadcrumbs.add( new Link("Actions", createURL( siteGroup.getGroupId() ) ) );
			
			data.put("breadcrumbs", breadcrumbs);
			data.put("title", "Incident Response Actions");
			Shortcuts.addDashboardHeaders(request, response, data);
			
			TemplateLoader.renderToResponse("ResponseActionList.ftl", data, response);
			return true;
		}
		catch( NoDatabaseConnectionException e ){
			throw new ViewFailedException(e);
		}
		catch( SQLException e ){
			throw new ViewFailedException(e);
		}
		catch( InputValidationException e ){
			throw new ViewFailedException(e);
		}
	}

}

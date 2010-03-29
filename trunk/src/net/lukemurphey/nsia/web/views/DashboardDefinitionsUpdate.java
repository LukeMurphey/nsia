package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSet;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardDefinitionsUpdate extends View {

	private static long lastChecked = 0;
	private static boolean newerAvailable = false;
	private static Thread checkerThread;
	private static boolean checkingVersion = false;
	private static Date currentDefinitionsDate = null;
	private static DefinitionVersionID currentDefinitionsID = null;
	private static DefinitionVersionID loadedVersionID = null;
	
	
	public static final int CHECK_FREQUENCY_SECS = 30 * 60; //Half an hour
	
	public DashboardDefinitionsUpdate() {
		super("DashboardPanel/UpdatedDefinitions", "dashboard_panel_definition_updates");
		isNewerVersionAvailable(); // This kicks off the definitions update check
	}

	private class VersionChecker extends Thread {
		public void run(){
			checkingVersion = true;
			try{
				DefinitionArchive archive = DefinitionArchive.getArchive();
				loadedVersionID = archive.getVersionID();
				
				DefinitionSet definitionSet = archive.getDefinitionSet();
				newerAvailable = archive.isNewDefinitionSetAvailable();
				currentDefinitionsDate = definitionSet.getDefinitionSetDate();
				currentDefinitionsID = DefinitionArchive.getLatestAvailableDefinitionSetID();
			}
			catch( DefinitionSetLoadException e ){
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			} catch (SQLException e) {
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			} catch (NoDatabaseConnectionException e) {
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			} catch (InputValidationException e) {
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			} catch (XmlRpcException e) {
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			} catch (IOException e) {
				Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
			}
			finally{
				checkingVersion = false;
				lastChecked = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * Determines if the definitions changed since the last time that the check was performed.
	 * @return
	 */
	protected boolean definitionsChanged(){
		if( loadedVersionID == null ){
			return true;
		}
		
		try{
			DefinitionArchive archive = DefinitionArchive.getArchive();
			if( loadedVersionID.equals( archive.getVersionID() ) == false ){
				return true;
			}
			else{
				return false;
			}
		}
		catch( DefinitionSetLoadException e ){
			Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
		}
		catch (SQLException e) {
			Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
		}
		catch (NoDatabaseConnectionException e) {
			Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
		}
		catch (InputValidationException e) {
			Application.getApplication().getEventLog().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR) , e);
		}
		
		return false;
	}
	
	protected synchronized boolean isNewerVersionAvailable(){
		if( definitionsChanged() || (
				checkingVersion == false
				&& lastChecked < (System.currentTimeMillis() - CHECK_FREQUENCY_SECS * 1000) ) ){
			checkerThread = new VersionChecker();
			checkerThread.setName("Definitions Update Version Checker");
			checkerThread.start();
			return false;
		}
		else{
			return newerAvailable;
		}
	}
	
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
		
		try {
			if( Application.getApplication().getApplicationConfiguration().getAutoDefinitionUpdating() == true ){
				return null; //Don't bother showing the panel if updates occur automatically
			}
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		if( isNewerVersionAvailable() && currentDefinitionsID != null ){
			data.put("new_version", currentDefinitionsID.toString());
			data.put("new_version_date", currentDefinitionsDate);
			return TemplateLoader.renderToString("DashboardDefinitionUpdate.ftl", data);
		}
		else{
			return null;
		}
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		String panel = getPanel(request, data, Application.getApplication());
		
		if( panel != null ){
			response.getOutputStream().print(panel);
		}
		
		return true;
		
	}

}

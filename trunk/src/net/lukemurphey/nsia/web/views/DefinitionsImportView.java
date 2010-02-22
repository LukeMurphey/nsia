package net.lukemurphey.nsia.web.views;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionArchiveException;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DefinitionsImportView extends View {

	public static final String VIEW_NAME = "definitions_import" ;
	
	public DefinitionsImportView() {
		super("Definitions/Import", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		DefinitionsImportView view = new DefinitionsImportView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		// 1 -- Check rights
		//TODO check rights
		//checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		// 2 -- Import the definitions (if a file was provided)
		if( request.getMethod().equalsIgnoreCase("POST") ){
			
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			
			if( isMultipart ){
				ServletFileUpload upload = new ServletFileUpload();
				
				try{
					FileItemIterator iter = upload.getItemIterator(request);
					
					while ( iter.hasNext() ) {
					    FileItemStream item = iter.next();
					    
					    // Get the file and import it
					    if( item.getFieldName().equalsIgnoreCase("DefinitionsFile") ){
					    	InputStream stream = item.openStream();
					    	
					    	// Get the file as a string
					    	String xmlString = getString( stream, 16000000 );
					    	
					    	// Import the XML string
					    	DefinitionArchive archive = DefinitionArchive.getArchive();
							archive.updateDefinitions(xmlString, false);
							
							// Log that the import occurred
							Application.getApplication().logEvent(EventLogMessage.Category.DEFINITIONS_UPDATED, new EventLogField[]{
									new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
									new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() ),
									new EventLogField( EventLogField.FieldName.IMPORT_SOURCE, "Local file" )} );
							
							// Give the user a message
							context.addMessage("Definitions successfully imported", MessageSeverity.SUCCESS);
							response.sendRedirect( StandardViewList.getURL(DefinitionsView.VIEW_NAME) );
							return true;
					    }
					}
				}
				catch(FileUploadException e){
					context.addMessage("Definition file upload failed", MessageSeverity.WARNING);
				}
		    	catch(DefinitionSetLoadException e){
		    		context.addMessage("Definition file is not valid", MessageSeverity.WARNING);
		    	}
		    	catch(DefinitionArchiveException e){
		    		context.addMessage("Definition file is not valid", MessageSeverity.WARNING);
		    	} catch (SQLException e) {
		    		throw new ViewFailedException(e);
				} catch (NoDatabaseConnectionException e) {
					throw new ViewFailedException(e);
				} catch (InputValidationException e) {
					throw new ViewFailedException(e);
				}
			}
		}
		
		// 3 -- Show the import form if no file was provided
		//Add the Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Definitions", StandardViewList.getURL(DefinitionsView.VIEW_NAME)) );
		breadcrumbs.add(  new Link("Import Definitions", createURL()) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Add the Menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("System Administration") );
		menu.add( new Link("System Status", StandardViewList.getURL("system_status")) );
		menu.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
		menu.add( new Link("Event Logs", StandardViewList.getURL("event_log")) );
		menu.add( new Link("Shutdown System", StandardViewList.getURL("system_shutdown")) );
		menu.add( new Link("Create Backup", StandardViewList.getURL("system_backup")) );
		
		menu.add( new Link("Scanning Engine") );
		if( Application.getApplication().getScannerController().scanningEnabled() ){
			menu.add( new Link("Stop Scanner", StandardViewList.getURL("scanner_stop")) );
		}
		else{
			menu.add( new Link("Start Scanner", StandardViewList.getURL("scanner_start")) );
		}
		menu.add( new Link("View Definitions", StandardViewList.getURL(DefinitionsView.VIEW_NAME)) );
		
		menu.add( new Link("Definitions") );
		menu.add( new Link("Update Now", StandardViewList.getURL(DefinitionsUpdateView.VIEW_NAME)) );
		menu.add( new Link("Create New Definition", StandardViewList.getURL(DefinitionEntryView.VIEW_NAME, "New")));
		menu.add( new Link("Export Custom Definitions", StandardViewList.getURL(DefinitionsExportView.VIEW_NAME) ));
		
		data.put("menu", menu);
		
		Shortcuts.addDashboardHeaders(request, response, data);
		data.put("title", "Import Definitions");
		
		TemplateLoader.renderToResponse("DefinitionsImport.ftl", data, response);
		
		return true;
	}
	
	private static String getString( InputStream in, int bytesLimit) throws IOException{
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			bos.write(buf, 0, len);
		}
				
		String fileData = new String(bos.toByteArray());
		return fileData;
	}

}

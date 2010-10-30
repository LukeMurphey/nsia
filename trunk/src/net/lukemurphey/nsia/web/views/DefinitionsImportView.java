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
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionArchiveException;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
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
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Import definitions") == false ){
				context.addMessage("You do not have permission to import definitions", MessageSeverity.WARNING);
				response.sendRedirect( DefinitionsView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
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
							Application.getApplication().logEvent(EventLogMessage.EventType.DEFINITION_SET_UPDATED, new EventLogField[]{
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
		data.put("menu", Menu.getDefinitionMenu(context));
		
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

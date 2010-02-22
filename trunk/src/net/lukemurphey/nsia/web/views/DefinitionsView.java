package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSet;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DefinitionsView extends View {

	public static final int DEFINITIONS_PER_PAGE = 25;
	public static final String VIEW_NAME = "definitions_list";
	
	public DefinitionsView() {
		super("Definitions", VIEW_NAME);
	}
	
	public static String getURL() throws URLInvalidException{
		DefinitionsView view = new DefinitionsView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		try{
			// 1 -- Make sure the user has permission
			//checkRight( context.getSessionInfo(), "System.Configuration.View");
			
			
			// 2 -- Determine what the user has requested (from the parameters)
			boolean endRequested = false;
			int page = 1;
			
			if( request.getParameter("N") != null && request.getParameter("N").equalsIgnoreCase("[End]") ){
				endRequested = true;
			}
			else if( request.getParameter("N") != null ){
				
				try{
					page = Integer.parseInt( request.getParameter("N") );
				}
				catch(NumberFormatException e){
					//Ignore this exception, we will just show the first page of definitions
				}
				
				if( page < 1){
					page = 1;
				}
			}
			
			String filter = request.getParameter("Filter");
			boolean notFilter = request.getParameter("Not") != null;
			
			
			// 3 -- Get the definitions
			Vector<Definition> definitionsList = new Vector<Definition>();
			
			DefinitionArchive archive = DefinitionArchive.getArchive();
			int definitionsMatching = 0;
			int start;
			int end;
			Definition[] definitions = archive.getDefinitionsSorted();
			
			if( definitions == null ){
				definitions = new Definition[0];
			}
			
			// Get the resulting list of definitions
			if( filter == null || filter.isEmpty() ){
				if( endRequested){
					page = (int)Math.ceil((double)definitions.length/DEFINITIONS_PER_PAGE);
				}
				
				start = DEFINITIONS_PER_PAGE * (page-1);
				
				for( int c = start; definitionsMatching < DEFINITIONS_PER_PAGE && c < definitions.length; c++){
					definitionsList.add(definitions[c]);
					definitionsMatching++;
				}
				
				definitionsMatching = definitions.length;
			}
			else{

				//If the end of the list is requested, calculate where the page should begin loading the results
				if( endRequested ){
					for( int c = 0; c < definitions.length; c++){
						if( (
								( notFilter == false && StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
								|| (notFilter == true && !StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
						   ) ){
							definitionsMatching++;
						}
					}
					start = definitionsMatching - (definitionsMatching % DEFINITIONS_PER_PAGE);
					end = start + DEFINITIONS_PER_PAGE;
					
					definitionsMatching = 0;
				}
				else{
					start = DEFINITIONS_PER_PAGE * (page-1);
					end = start + DEFINITIONS_PER_PAGE;
				}
			
				//Get the matching definitions
				for( int c = 0; c < definitions.length; c++){
					if( (
							( notFilter == false && StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
							|| (notFilter == true && !StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
					   ) ){
						definitionsMatching++;
						
						if( definitionsMatching >= start && definitionsMatching <= end ){
							definitionsList.add(definitions[c]);
						}
					}
				}
			}
			
			
			// 4 -- Get information about the definition set
			DefinitionSet definitionSet = archive.getDefinitionSet();
			
			data.put("updated", definitionSet.getDefinitionSetDate());
			data.put("newer_definitions_available", archive.isNewDefinitionSetAvailable());
			
			try {
				data.put("latest_definitions", DefinitionArchive.getLatestAvailableDefinitionSetID());
			} catch (XmlRpcException e) {
				//Unable to get information on newest version
			}
			
			data.put("definition_set_version", definitionSet.getVersionID());
			data.put("filter", filter);
			data.put("not_filter", notFilter);
			data.put("definitions", definitionsList);
			
			
			// 5 -- Get the data for the pagination
			int startPage;
	        int totalPages = (int)Math.ceil( (double)definitionsMatching / DEFINITIONS_PER_PAGE );
	        int currentPage = page - 1;
	        
	        startPage = Math.max( currentPage-3, 1);
			if( totalPages > 10 && ( startPage + 9 ) > totalPages ){
				startPage = Math.max( totalPages-9, 1);
			}
			
			data.put("total_entries", 12);
			data.put("entries_per_page", DEFINITIONS_PER_PAGE);
			data.put("current_page", 12);
			data.put("total_entries", definitionsMatching);
			data.put("start_page", startPage);
			data.put("total_pages", totalPages);
			
			Vector<Integer> pageNums = new Vector<Integer>();
			for(int c = startPage; c < (startPage+10) && c <= totalPages; c++){
				pageNums.add(c);
			}
			
			data.put("page_numbers", pageNums);
			
			if((startPage + 10) < totalPages ){
				data.put("needs_end_marker", true);
			}
			
			// 6 -- Render the resulting page
			data.put("title", "Definitions");
			
			//Add the Breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add(  new Link("Definitions", createURL()) );
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
			
			menu.add( new Link("Definitions") );
			menu.add( new Link("Update Now", StandardViewList.getURL(DefinitionsUpdateView.VIEW_NAME)) );
			menu.add( new Link("Create New Definition", StandardViewList.getURL(DefinitionEntryView.VIEW_NAME, "New")));
			menu.add( new Link("Import Definitions", StandardViewList.getURL(DefinitionsImportView.VIEW_NAME) ));
			menu.add( new Link("Export Custom Definitions", StandardViewList.getURL(DefinitionsExportView.VIEW_NAME) ));
			menu.add( new Link("Edit Default Policy", StandardViewList.getURL(DefinitionPolicyView.VIEW_NAME) ));
			
			data.put("menu", menu);
			
			//Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			TemplateLoader.renderToResponse("DefinitionsList.ftl", data, response);
			
			return true;
		}
		catch(InputValidationException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(DefinitionSetLoadException e){
			throw new ViewFailedException(e);
		}
	}

}

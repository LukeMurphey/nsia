package net.lukeMurphey.nsia.htmlInterface;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.DisallowedOperationException;
import net.lukeMurphey.nsia.DuplicateEntryException;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.WorkerThread;
import net.lukeMurphey.nsia.scanRules.Definition;
import net.lukeMurphey.nsia.trustBoundary.ApiDefinitionSet;
import net.lukeMurphey.nsia.scanRules.InvalidDefinitionException;
import net.lukeMurphey.nsia.scanRules.ScriptDefinition;
import net.lukeMurphey.nsia.scanRules.DefinitionArchiveException;
import net.lukeMurphey.nsia.scanRules.DefinitionSetLoadException;
import net.lukeMurphey.nsia.scanRules.DefinitionUpdateFailedException;
import net.lukeMurphey.nsia.scanRules.PatternDefinition;
import net.lukeMurphey.nsia.scanRules.Definition.Reference;
import net.lukeMurphey.nsia.scanRules.DefinitionSet.DefinitionVersionID;
import net.lukeMurphey.nsia.scanRules.UnpurposedDefinitionException;
import net.lukeMurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukeMurphey.nsia.WorkerThread.State;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;

import java.util.Date;

import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.*;
import java.io.*;


public class HtmlDefinitionSet extends HtmlContentProvider {

	private static int OP_EXPORT = 101;
	private static int OP_IMPORT = 102;
	private static int OP_IMPORT_SUCCESS = 103;
	private static int OP_IMPORT_FAILED = 104;
	private static int OP_DEFINITIONS_UPDATE_FAILED = 105;
	private static int OP_DEFINITIONS_UPDATE_SUCCESS = 106;
	private static int OP_DEFINITIONS_UPDATE_STARTED = 107;
	private static int OP_DEFINITIONS_UPDATE_IN_PROCESS = 108;
	
	private final static int DEFINITIONS_PER_PAGE = 20;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		
		// 0 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor );
		
		// 1 -- Determine if a scheduled definition update was initiated
		//WorkerThreadDescriptor[] workerThreads = Application.getApplication().getWorkerThreadQueue();
		WorkerThread worker = null;
		WorkerThreadDescriptor desc = null;
		desc = Application.getApplication().getWorkerThread("Definitions Update (unscheduled)");
		
		if( desc != null ){
			worker = desc.getWorkerThread();
		}
		
		/*for( int c = 0; c < workerThreads.length; c++){
			if( workerThreads[c].getWorkerThread().getTaskDescription().equalsIgnoreCase("Definitions Update (unscheduled)")){
				worker = workerThreads[c].getWorkerThread();
			}
		}*/
		
		// 2 -- Perform the relevant operation
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == ActionDescriptor.OP_ADD_FAILED ){
			return getDefinitionsNew(requestDescriptor, actionDesc );
		}
		else if( actionDesc.result == ActionDescriptor.OP_UPDATE || actionDesc.result == ActionDescriptor.OP_UPDATE_FAILED ){
			return getDefinitionEdit(requestDescriptor, actionDesc );
		}
		else if( actionDesc.result == OP_EXPORT ){
			return new ContentDescriptor("Export", actionDesc.addData.toString(), false);
		}
		else if( actionDesc.result == OP_IMPORT ){
			return getImportForm(requestDescriptor, actionDesc);
		}
		else if( (worker!= null && worker.getStatus() != State.STOPPED) || actionDesc.result == OP_DEFINITIONS_UPDATE_STARTED ){
			return getProgressDialog(requestDescriptor, actionDesc, desc);
		}
		else{
			return getDefinitionsView(requestDescriptor, actionDesc );
		}

	}
	
	private static StringBuffer createRuleSelectRow( String name, String description, String link){
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("<div style=\"padding:5px;\">");
		buffer.append("<div class=\"ToolIcon\" style=\"float:left;\"><a href=\"" + link + "\"><img src=\"/32_Add\"></a></div>");
		
		buffer.append("<div style=\"position:relative; left:8px;\"><a href=\"" + link + "\"><span class=\"Text_2\">").append(name).append("</span></a><br>").append(description).append("</div>");
		buffer.append("<br></div>");
		
		return buffer;
		
	}
	
	private static StringBuffer createRow( Definition definition, boolean isEven ){
		
		// 1 -- Display the definition information
		StringBuffer buffer = new StringBuffer();
		
		if( isEven ){
			buffer.append("<tr class=\"even\">");
		}
		else{
			buffer.append("<tr class=\"odd\">");
		}
		if( definition instanceof ScriptDefinition){
			buffer.append("<td>&nbsp;<img style=\"vertical-align: top;\" src=\"/16_script\" alt=\"script\">&nbsp;" + definition.toString() + "&nbsp;&nbsp;&nbsp;</td>");
		}
		else if( definition instanceof PatternDefinition){
			buffer.append("<td>&nbsp;<img style=\"vertical-align: top;\" src=\"/16_plugin\" alt=\"script\">&nbsp;" + definition.toString() + "&nbsp;&nbsp;&nbsp;</td>");
		}
		//buffer.append("<td style=\"height:26px;\">" + definition.getMessage() + "&nbsp;</td>");
		if( definition.isOfficial() ){
			buffer.append("<td>Official&nbsp;&nbsp;</td>");
		}
		else{
			buffer.append("<td>Local&nbsp;&nbsp;</td>");
		}
		
		//If the definition is official, then don't present the edit and delete buttons since these definitions cannot be deleted
		if( definition.isOfficial() ){
			buffer.append("<td colspan=\"2\">&nbsp;&nbsp;<a href=\"/Definitions?Action=Edit&ID=" + definition.getLocalID() + "\"><img class=\"imagebutton\"src=\"/16_magnifier\" alt=\"View\"><span style=\"vertical-align:top;\">&nbsp;View</span></a>&nbsp;</td>");
		}
		else{
			buffer.append("<td>&nbsp;&nbsp;<a href=\"/Definitions?Action=Edit&ID=" + definition.getLocalID() + "\"><img class=\"imagebutton\"src=\"/16_Configure\" alt=\"Edit\"><span style=\"vertical-align:top;\">&nbsp;Edit</span></a>&nbsp;</td>");
			buffer.append("<td>&nbsp;&nbsp;<a href=\"/Definitions?Action=Delete&ID=" + definition.getLocalID() + "\"><img class=\"imagebutton\"src=\"/16_Delete\" alt=\"Delete\"><span style=\"vertical-align:top;\">&nbsp;Delete</span>&nbsp;</a></td>");
		}
		buffer.append("</tr>");
		
		return buffer;
		
	}
	
	private static ContentDescriptor getProgressDialog( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, WorkerThreadDescriptor worker ){
		
		//return HtmlOptionDialog.getHtml(requestDescriptor, "Updating Definitions", "The definitions are currently being updated. ", new Hashtable<String, String>(), new String[]{"Cancel"}, "Definitions", HtmlOptionDialog.DIALOG_INFORMATION, -1, "https://127.0.0.1:8443/Signatures");
		return HtmlOptionDialog.getHtml(requestDescriptor, "Updating Definitions", "The definitions are currently being updated. ", new Hashtable<String, String>(), new String[]{"Cancel"}, "Definitions", HtmlOptionDialog.DIALOG_INFORMATION, "Definitions", "/Ajax/Task/" + worker.getUniqueName());
	}
	
	private static ContentDescriptor getImportForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Display the form
		body.append( "<span class=\"Text_1\">Upload the Definitions File</span><br>Select the definitions file to upload in order to incorporate the definitions into the active set<br>&nbsp;<br>" );
		body.append("<form method=\"post\" enctype=\"multipart/form-data\" action=\"/Definitions?Action=Import\"><input type=\"file\" name=\"DefinitionsFile\" />&nbsp;&nbsp;");
		body.append("<input type=\"submit\" value=\"Upload\"></form>");
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Definitions", "/Definitions");
		navPath.addPathEntry( "Import Definitions", "/Definitions?Action=Import");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Definitions", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Update Now", "/Definitions?Action=Update", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Create New Definition", "/Definitions?Action=New", MenuItem.LEVEL_TWO) );
		
		ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
		
		try{
			if( definitionSet.getCustomDefinitionsCount( requestDescriptor.sessionIdentifier) > 0 ){
				menuItems.add( new MenuItem("Export Custom Definitions", "/Definitions?Action=Export", MenuItem.LEVEL_TWO) );
			}
		}catch(InsufficientPermissionException e){
			//Ignore this, instead just don't show the option to export the custom definitions
		}
		
		menuItems.add( new MenuItem("Import Definitions", "/Definitions?Action=Import", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Definitions", pageOutput);
		
	}
	
	private static String getPageIndex( String preLinkString, int page, int totalEntries, int entriesPerPage, String filter, boolean notFilter ){
		
		if( entriesPerPage >= totalEntries ){
			return "";
		}
		
		StringBuffer result = new StringBuffer();
		String separator = "&nbsp;";
		int currentPage = page - 1;
		
		result.append("<form action=\"/Definitions\" method=\"Post\">");
		
		int startPage;
		int totalPages = (int)Math.ceil( (double)totalEntries / entriesPerPage );
		
		// Reset the current page if the one given is past the end
		/*if(page > totalPages){
			page = totalPages;
		}*/
		
		startPage = Math.max( currentPage-3, 1);
		if( totalPages > 10 && ( startPage + 9 ) > totalPages ){
			startPage = Math.max( totalPages-9, 1);
		}
		
		if( startPage > 1 ){
			result.append("<input style=\"width:64px;\" class=\"button\" type=\"submit\" name=\"N\" value=\"[Start]\">");
			result.append(separator);
		}
		
		for(int c = startPage; c < (startPage+10) && c <= totalPages; c++){
			result.append("<input style=\"width:32px;\" class=\"button\" type=\"submit\" name=\"N\" value=\"" + ( c ) + "\">");
			result.append(separator);
		}
		
		
		if( (startPage + 10) < totalPages ){
			result.append("<input style=\"width:64px;\" class=\"button\" type=\"submit\" name=\"N\" value=\"[End]\">");
		}
		
		if( filter != null ){
			if( notFilter == true ){
				result.append("<input type=\"hidden\" name=\"Not\" value=\"Not\">");
			}
			
			result.append("<input type=\"hidden\" name=\"Filter\" value=\"" + filter + "\">");
		}
		
		/*int c;
		for(c = 0; c < 10 && (c+currentPage) < totalPages; c++){
			//result.append("<a href=\"" + preLinkString + (currentPage + c) + "\">[" + (c + currentPage) +  "]</a>");
			result.append("<input style=\"width:32px;\" class=\"button\" type=\"submit\" name=\"N\" value=\"" + (page + c ) + "\">");
			result.append(separator);
		}
		
		if( c < totalPages ){
			//result.append("<a href=\"" + preLinkString + (c+1) + "\">[Next]</a>");
			result.append("<input style=\"width:64px;\" class=\"button\" type=\"submit\" name=\"N\" value=\"[End]\">");
		}*/
		
		return result.toString();
		
	}
	
	private static ContentDescriptor getDefinitionsView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Get a list of the definitions
		
		//	 2.1 -- Determine which page of definitions to show
		int page = 1;
		int start;
		int end;
		
		ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
		
		Definition[] definitions = null;
		try{
			
			// 2.2 -- Get the definitions and add them to the view
			definitions = definitionSet.getSortedDefinitions( requestDescriptor.sessionIdentifier );
			
			if( definitions == null || definitions.length == 0 ){
				body.append( Html.getDialog("No definitions exist yet. Download updated definitions to get the most current official set.<p><a href=\"/Definitions?Action=Update\">[Update Definitions Now]</a>", "No Definitions", "/32_Information", false) );
			}
			else{
				
				// 2.2.1 -- Identify what the user is requesting (get the parameters)
				boolean endRequested = false;
				
				if( requestDescriptor.request.getParameter("N") != null && requestDescriptor.request.getParameter("N").equalsIgnoreCase("[End]") ){
					endRequested = true;
				}
				else if( requestDescriptor.request.getParameter("N") != null ){
					
					try{
						page = Integer.parseInt( requestDescriptor.request.getParameter("N") );
					}
					catch(NumberFormatException e){
						//Ignore this exception, we will just show the first page of definitions
					}
					
					if( page < 1){
						page = 1;
					}
				}
				
				String filter = requestDescriptor.request.getParameter("Filter");
				boolean notFilter = requestDescriptor.request.getParameter("Not") != null;
				
				// 2.2.2 -- Show the page header and description
				Date updated =  definitionSet.getDefinitionSetDate(requestDescriptor.sessionIdentifier);
				

				
				if( updated != null ){
					
					body.append( "<span class=\"Text_1\">Definitions</span><br>" );
					//body.append( definitions.length + " definitions (Last Updated " + updated + ")<br>&nbsp;<br>" );
					body.append( "Revision " + definitionSet.getDefinitionSetVersionID(requestDescriptor.sessionIdentifier) + " (Last Updated " + updated + ")<br>&nbsp;<br>" );
				}
				else{
					body.append( "<span class=\"Text_1\">Definitions</span><br>" );
					body.append( definitions.length + " definitions total (Last Update Undefined)<br>&nbsp;<br>" );
				}
				
				//Show a message indicating that new definitions exist (if they do)
				if( definitionSet.areNewDefinitionsAvailable(requestDescriptor.sessionIdentifier) == true ){
					//body.append( Html.getWarningNote("New definitions are available (version " + definitionSet.getLatestAvailableDefinitionSetID(requestDescriptor.sessionIdentifier) + ")") );
					//body.append( "<span style=\"position: relative; left: 24px;\"><a href=\"/Definitions?Action=Update\">[Load New Definitions Now]</a></span><p/>" );
					
					body.append( Html.getWarningDialog("Current Definitions Obsolete", "Newer definitions are available (version " + definitionSet.getLatestAvailableDefinitionSetID(requestDescriptor.sessionIdentifier) + ") <a href=\"/Definitions?Action=Update\">[Update Now]</a><p/>") );
				}
				
				// 2.2.3 -- Note if the definitions are outdated
				/*boolean outdated = true;
				
				if( outdated ){
					body.append( GenericHtmlGenerator.getDialog("The current definitions are outdated. Download updated definitions to get the most current official definition set.<p><a href=\"/Definitions?Action=Update\">[Update Definitions Now]</a>", "Definitions Outdated", "/32_Warning", false) );
				}*/
				
				// 2.2.4 -- Show the filtering form
				body.append( "<form method=\"post\" action=\"/Definitions\"><input class=\"button\" type=\"Submit\" value=\"Filter\">");
				if( filter == null ){
					body.append("<input class=\"textInput\" type=\"text\" width=\"32\" name=\"Filter\">" );
				}
				else{
					body.append("<input class=\"textInput\" type=\"text\" width=\"32\" name=\"Filter\" value=\"" + filter + "\">" );
				}
				
				if( requestDescriptor.request.getParameter("Not") != null ){
					body.append( "<input id=\"NotFilter\" type=\"checkbox\" name=\"Not\" checked><label for=\"NotFilter\">Exclude (excludes items based on the filter)</label></input>" );
				}
				else{
					body.append( "<input id=\"NotFilter\" type=\"checkbox\" name=\"Not\"><label for=\"NotFilter\">Exclude (excludes items based on the filter)</label></input>" );
				}
				
				
				body.append( "</form>" );
				
				/*if(filter != null){
					body.append(GenericHtmlGenerator.getDialog("Displaying definitions that contain \"<u>" + StringEscapeUtils.escapeHtml(filter) + "</u>\".<br><a href=\"/Definitions\">[Clear Filter]</a>", "Filters Applied", "/32_Information") );
				}*/
				
				// 2.2.5 -- Show the table of definitions
				
				
				int definitionsMatching = 0;
				
				boolean tableStartShown = false;
				
				if( filter == null || filter.isEmpty() ){
					
					if( endRequested){
						page = (int)Math.ceil((double)definitions.length/DEFINITIONS_PER_PAGE);
					}
					
					start = DEFINITIONS_PER_PAGE * (page-1);
					//end = start + DEFINITIONS_PER_PAGE;
					
					for( int c = start; definitionsMatching < DEFINITIONS_PER_PAGE && c < definitions.length; c++){
						
						if(tableStartShown == false){
							body.append("<table class=\"DataTable\"><thead><tr><td>Name</td><td>Type</td><td colspan=\"2\">Options</td></tr></thead>");
							body.append( "<tbody>" );
							tableStartShown = true;
						}
						
						body.append( createRow( definitions[c], (c%2) != 0 ) );
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
				
					//Output the results
					for( int c = 0; c < definitions.length; c++){
						if( (
								( notFilter == false && StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
								|| (notFilter == true && !StringUtils.containsIgnoreCase( definitions[c].getFullName(), filter) )
						   ) ){
							definitionsMatching++;
							
							if( definitionsMatching >= start && definitionsMatching <= end ){
								
								if(tableStartShown == false){
									body.append("<table class=\"DataTable\"><thead><tr><td>Name</td><td>Type</td><td colspan=\"2\">Options</td></tr></thead>");
									body.append( "<tbody>" );
									tableStartShown = true;
								}
								
								body.append( createRow( definitions[c], (definitionsMatching%2) != 0 ) );
							}
						}
					}
					
					if( tableStartShown == false ){
						body.append( Html.getDialog("No definitions match the filter. <p><a href=\"/Definitions\">[Clear Filter]</a>", "No Matches Found", "/32_Information", false) );
					}
				}
				
				if( tableStartShown == true ){
					body.append( "</tbody>" );
					body.append("</table><p>");
				}
				
				//body.append( "<p>Pages (" + definitions.length + " total definitions):<p>" );
				body.append( getPageIndex("Definitions?N=", page, definitionsMatching, DEFINITIONS_PER_PAGE, filter, notFilter ) );
			}
			
		}catch( InsufficientPermissionException e){
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the definitions", "Console", "Return to Main Dashboard"));
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Definitions", "/Definitions");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Definitions", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Update Now", "/Definitions?Action=Update", MenuItem.LEVEL_TWO, "showHourglass('Updating...');") );
		menuItems.add( new MenuItem("Create New Definition", "/Definitions?Action=New", MenuItem.LEVEL_TWO) );
		
		try{
			if( definitionSet.getCustomDefinitionsCount( requestDescriptor.sessionIdentifier) > 0 ){
				menuItems.add( new MenuItem("Export Custom Definitions", "/Definitions?Action=Export", MenuItem.LEVEL_TWO) );
			}
		}catch(InsufficientPermissionException e){
			//Ignore this, instead just don't show the option to export the custom definitions
		}
		
		menuItems.add( new MenuItem("Import Definitions", "/Definitions?Action=Import", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Definitions", pageOutput);
		
	}
	
	private static StringBuffer getThreatSignatureEditorForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		return getThreatSignatureEditorForm( requestDescriptor, actionDesc, null);
	}
	
	private static StringBuffer getReferenceList( Reference[] references ){
		StringBuffer body = new StringBuffer();
		
		if( references.length > 0 ){
		
			for(int c = 0; c < references.length; c++){
				body.append( "<table><tr><td><img style=\"margin: 0px\" src=\"/16_Bullet_arrow\"></td><td>");
				if( references[c].getType() == Reference.ARACHNIDS){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\"> Arachnids " + references[c].getValue() + "</a><br>" );
				}
				else if( references[c].getType() == Reference.BUGTRAQ){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\">BID " + references[c].getValue() + "</a><br>" );
				}
				else if( references[c].getType() == Reference.CVE){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\">CVE " + references[c].getValue() + "</a><br>" );
				}
				else if( references[c].getType() == Reference.MCAFEE){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\">Mcafee " + references[c].getValue() + "</a><br>" );
				}
				else if( references[c].getType() == Reference.NESSUS){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\">Nessus " + references[c].getValue() + "</a><br>" );
				}
				else{//( references[c].getType() == Reference.URL){
					body.append( "<a href=\"" + references[c].getType().getUrlPrefix() + references[c].getValue() + "\">" + references[c].getValue() + "</a><br>" );
				}
				
				body.append( "</td><tr></table>");
			}
		}
		
		return body;
	}
	
	private static StringBuffer getDefinitionInformation(Definition definition){
		StringBuffer body = new StringBuffer();
		
		body.append( "<div style=\"position:relative;\">" );
		
		//body.append("<div><table cellpadding=\"2\">");
		body.append("<div style=\"position: relative; left: 0px;\"><table cellpadding=\"2\">");
		body.append("<tr><td class=\"Text_2\">Name</td><td>&nbsp;</td><td>" + StringEscapeUtils.escapeHtml(definition.getFullName()) + "</td></tr>");
		body.append("<tr><td class=\"Text_2\">Message</td><td>&nbsp;</td><td>" + StringEscapeUtils.escapeHtml(definition.getMessage()) + "</td></tr>");
		body.append("<tr><td class=\"Text_2\">Severity</td><td>&nbsp;</td><td>" + StringEscapeUtils.escapeHtml(definition.getSeverity().name()) + "</td></tr>");
		body.append("<tr><td class=\"Text_2\">Version</td><td>&nbsp;</td><td>" + definition.getRevision() + "</td></tr>");
		if( definition.getReferences() != null && definition.getReferences().length > 0 ){
			body.append("<tr><td class=\"Text_2\">References</td><td>&nbsp;</td><td>" + getReferenceList(definition.getReferences()) + "</td></tr>");
		}
		body.append("</table><p>");
		
		return body;
	}
	
	private static StringBuffer getThreatSignatureEditorForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, PatternDefinition threatSignature ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Determine if the form is editing an existing rule
		String code = null;
		
		if( threatSignature != null ){
			code = threatSignature.getRuleCode();
		}
		
		//	 1.1 -- Get the signature code from the request arguments
		if( requestDescriptor.request.getParameter("SignatureCode") != null ){
			code = requestDescriptor.request.getParameter("SignatureCode");
		}
		else if( requestDescriptor.request.getParameter("SignatureCode2") != null ){
			code = requestDescriptor.request.getParameter("SignatureCode2");
		}
		else if( code == null ){
			ApiDefinitionSet set = new ApiDefinitionSet(Application.getApplication());
			code = "Alert(\"[Insert name here]\"){\n	ID=\"" + set.getNextOpenID(requestDescriptor.sessionIdentifier) + "\"\n	Message=\"[Insert message here]\";\n	Severity=\"Medium\";\n	Version=1;\n}";
		}
		
		// 1.2 -- Display the page title 
		body.append("<span class=\"Text_2\">ThreatPattern Definition</span><p>");
		
		// 1.3 -- Display the definition details
		if( threatSignature != null ){
			body.append(getDefinitionInformation(threatSignature));
		}

		//	 1.4 -- Display the code
		body.append( "<script src=\"/codepress/codepress.js\" type=\"text/javascript\"></script>");
		body.append( "<script type=\"text/javascript\">");
		body.append( "function submitEditorForm(editorform){");
		body.append( "document.editorform.SignatureCode2.value = cp1.getCode();");
		body.append( "document.editorform.submit();");
		body.append( "return true;");
		body.append( "}");
		body.append( "</script>");
		body.append( "<form name=\"editorform\" id=\"editorform\" action=\"/Definitions\" method=\"post\" onSubmit=\"return submitEditorForm(this.form)\"><textarea id=\"cp1\" class=\"codepress threatpattern");
		
		if( threatSignature != null && threatSignature.isOfficial() ){
			body.append(" readonly-on");
		}
		
		body.append( "\" wrap=\"off\" rows=\"18\" cols=\"90\" name=\"SignatureCode\">").append( StringEscapeUtils.escapeHtml( code ) ).append( "</textarea>" );
		
		if( threatSignature == null ){
			body.append( "<input type=\"hidden\" name=\"Action\" value=\"New\">" );
		}
		else{
			body.append( "<input type=\"hidden\" name=\"Action\" value=\"Edit\">" );
			body.append( "<input type=\"hidden\" name=\"ID\" value=\"" + threatSignature.getLocalID() + "\">" );
		}
		body.append( "<input type=\"hidden\" name=\"Type\" value=\"Pattern\">" );
		body.append( "<input type=\"hidden\" name=\"SignatureCode2\" value=\"" + StringEscapeUtils.escapeHtml( code )+ "\">" );
		
		
		// Include only a button to close the editor if the definition is part of the official set
		if(threatSignature != null && threatSignature.isOfficial() ){
			body.append( "<br><input class=\"button\" type=\"submit\" name=\"Cancel\" value=\"Close\"></form>");
		}
		else{
			body.append( "<br><input class=\"button\" type=\"submit\" name=\"Compile\" value=\"Compile\"><input class=\"button\" type=\"submit\" name=\"Cancel\" value=\"Cancel\"></form>");
		}
		
		return body;
	}
	
	private static StringBuffer getScriptEditorForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		return getScriptEditorForm( requestDescriptor, actionDesc, null);
	}
	
	private static StringBuffer getScriptEditorForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ScriptDefinition scriptSignature ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		
		StringBuffer body = new StringBuffer();
		
		// 1 -- Determine if the form is editing an existing rule
		int definitionID = -1;
		String code = null;
		
		if( scriptSignature != null ){
			code = scriptSignature.getScript();
			definitionID = scriptSignature.getLocalID();
		}
		
		//	 1.1 -- Get the definition code from the request arguments
		if( requestDescriptor.request.getParameter("SignatureCode") != null ){
			code = requestDescriptor.request.getParameter("SignatureCode");
		}
		else if( requestDescriptor.request.getParameter("SignatureCode2") != null ){
			code = requestDescriptor.request.getParameter("SignatureCode2");
		}
		else if( code == null ){
			ApiDefinitionSet set = new ApiDefinitionSet(Application.getApplication());
			
			code = "/*\n * Name: [Insert name here]\n * ID: " + set.getNextOpenID(requestDescriptor.sessionIdentifier) + "\n * Version: 1\n * Message: This is a test\n * Severity: Medium\n */\n\nimportPackage(Packages.ThreatScript);\n\nfunction analyze( httpResponse, operation, variables, environment, defaultRule ){\n	return new Result( false, \"Definition matched the input\");\n}";
		}
		
		
		//	 1.2 -- Display the references
		body.append("<span class=\"Text_2\">ThreatScript Definition</span><p>");
		
		if( scriptSignature != null ){
			body.append(getDefinitionInformation(scriptSignature));
			//body.append( getReferenceList( scriptSignature.getReferences() ) );
		}
		
		//	 1.3 -- Display the code
		body.append( "<script src=\"/codepress/codepress.js\" type=\"text/javascript\"></script>");
		body.append( "<script type=\"text/javascript\">");
		body.append( "function submitEditorForm(editorform){");
		body.append( "document.editorform.SignatureCode2.value = cp1.getCode();");
		//body.append( "alert(document.editorform.SignatureCode.value);");
		body.append( "document.editorform.submit();");
		body.append( "return true;");
		//body.append( "");
		body.append( "}");
		body.append( "</script>");
		body.append( "<p><form name=\"editorform\" id=\"editorform\" action=\"/Definitions\" method=\"post\" onSubmit=\"return submitEditorForm(this.form)\"><textarea id=\"cp1\" class=\"codepress javascript");
		
		if( scriptSignature != null && scriptSignature.isOfficial() ){
			body.append(" readonly-on");
		}
		body.append( "\" wrap=\"off\" rows=\"36\" cols=\"120\" name=\"SignatureCode\">").append( StringEscapeUtils.escapeHtml( code ) ).append( "</textarea>" );
		
		
		if( scriptSignature == null ){
			body.append( "<input type=\"hidden\" name=\"Action\" value=\"New\">" );
		}
		else{
			body.append( "<input type=\"hidden\" name=\"Action\" value=\"Edit\">" );
		}
		body.append( "<input type=\"hidden\" name=\"Type\" value=\"Script\">" );
		body.append( "<input type=\"hidden\" name=\"SignatureCode2\" value=\"" + StringEscapeUtils.escapeHtml( code )+ "\">" );
		
		if( definitionID > 0 ){
			body.append( "<input type=\"hidden\" name=\"ID\" value=\"" + definitionID + "\">" );
		}
		
		// Include only a button to close the editor if the definition is part of the official set
		if(scriptSignature != null && scriptSignature.isOfficial() ){
			body.append( "<br><input class=\"button\" type=\"submit\" name=\"Cancel\" value=\"Close\"></form>");
		}
		else{
			body.append( "<br><input class=\"button\" type=\"submit\" name=\"Compile\" value=\"Compile\"><input class=\"button\" type=\"submit\" name=\"Cancel\" value=\"Cancel\"></form>");
		}
		
		return body;
	}

	private static ContentDescriptor getDefinitionEdit(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Determine which form should be shown (select rule type or script definition)
		int definitionID = -1;
		
		//	 2.1 -- Try to load the definition from the database
		if( requestDescriptor.request.getParameter("ID") != null ){
			definitionID = Integer.parseInt(requestDescriptor.request.getParameter("ID"));
			
			ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
			
			try {
				Definition definition = definitionSet.getDefinition(requestDescriptor.sessionIdentifier, definitionID);
				
				if( definition instanceof ScriptDefinition ){
					body.append( getScriptEditorForm( requestDescriptor, actionDesc, (ScriptDefinition)definition) );
				}
				else if( definition instanceof PatternDefinition ){
					body.append( getThreatSignatureEditorForm( requestDescriptor, actionDesc, (PatternDefinition)definition) );
				}
			} catch (InsufficientPermissionException e) {
				body.append("<p>");
				body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the definitions", "Console", "Return to Main Dashboard"));
			} catch (NotFoundException e) {
				body.append("<p>");
				body.append(Html.getWarningDialog("Definition Not Found", "No definition was found with the given ID", "Console", "Return to Main Dashboard"));
			}
		}
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Definitions", "/Definitions");
		navPath.addPathEntry( "Edit Definition", "/Definitions?Action=Edit&ID=" + definitionID);
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Definitions", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Update Now", "/Definitions?Action=Update", MenuItem.LEVEL_TWO, "showHourglass('Updating...');") );
		menuItems.add( new MenuItem("Create New Definition", "/Definitions?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete Definition", "/Definitions?Action=Delete&ID=" + definitionID, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Definitions", pageOutput);
		
	}
	
	private static ContentDescriptor getDefinitionsNew(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Determine which form should be shown (select rule type or script definition)
		String type = requestDescriptor.request.getParameter("Type");
		
		//	 2.1 -- Show form to select rule type
		if( type == null || ( !type.equalsIgnoreCase( "pattern" ) && !type.equalsIgnoreCase( "script" ) )){
			body.append("<div style=\"padding:5px;\"><span class=\"Text_2\">Select Type of Rule To Add<span></div>");
			body.append( createRuleSelectRow("ThreatPattern Definition", "Patterns have a simple syntax and are useful for basic rules. Most issues can be detected with ThreatPatterns.", "Definitions?Action=New&Type=Pattern"));
			body.append( createRuleSelectRow("ThreatScript Definition", "Executable code can be written in the ThreatScript language to design an analysis engine capable of advanced analysis.", "Definitions?Action=New&Type=Script"));
			
		}
		//	 2.2 -- Show form to select rule type
		else if( type.equalsIgnoreCase( "pattern" ) ){
			body.append( getThreatSignatureEditorForm( requestDescriptor, actionDesc) );
		}
		//	 2.3 -- Show form to select rule type
		else if( type.equalsIgnoreCase( "script" ) ){
			body.append( getScriptEditorForm( requestDescriptor, actionDesc) );
		}
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Definitions", "/Definitions");
		navPath.addPathEntry( "New Definition", "/Definitions?Action=New");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Definitions", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Update Now", "/Definitions?Action=Update", MenuItem.LEVEL_TWO, "showHourglass('Updating...');") );
		menuItems.add( new MenuItem("Create New Definition", "/Definitions?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Definitions", pageOutput);
		
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
		
		// 1 -- User is creating a new definition
		else if( action.matches( "New" ) ){
			
			// 1.1 -- User has canceled creation of a new definition (opened the form, then pressed 'cancel')
			if( requestDescriptor.request.getParameter("Cancel") != null ){
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
			
			// 1.2 -- User has submitted a new definition
			if( requestDescriptor.request.getParameter("Compile") != null && (requestDescriptor.request.getParameter("SignatureCode") != null || requestDescriptor.request.getParameter("SignatureCode2") != null) && requestDescriptor.request.getParameter("Type") != null){
				String code;
				code = requestDescriptor.request.getParameter("SignatureCode");
				
				if( code == null ){
					code = requestDescriptor.request.getParameter("SignatureCode2");
				}
				
				Definition definition = null;
				
				try{
					if( requestDescriptor.request.getParameter("Type").equalsIgnoreCase("Pattern") ){
						definition = PatternDefinition.parse(code);
					}
					else if( requestDescriptor.request.getParameter("Type").equalsIgnoreCase("Script") ){
						definition = ScriptDefinition.parse(code);
					}
				}
				catch(InvalidDefinitionException e)
				{
					Html.addMessage(MessageType.WARNING, "The definition given is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				catch(UnpurposedDefinitionException e)
				{
					Html.addMessage(MessageType.WARNING, "The definition given is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				
				ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
				
				try {
					definitionSet.addDefinition(requestDescriptor.sessionIdentifier, definition);
				}
				catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING,  "You do not have permission to create new rules", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				catch (DuplicateEntryException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				catch (DisallowedOperationException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "The definition was added successfully", requestDescriptor.userId.longValue() );
				return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS );
			}
			
			return new ActionDescriptor(ActionDescriptor.OP_ADD);
		}
		
		// 2 -- User is editing a definition
		else if( action.matches( "Edit" ) ){
			
			// 2.1 -- User has canceled the editing of a definition (opened the form, then pressed 'cancel')
			if( requestDescriptor.request.getParameter("Cancel") != null ){
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
			
			// 2.2 -- Update the definition
			
			int definitionID = -1;
			
			//	 2.2.1 -- Get the definition identified
			if( requestDescriptor.request.getParameter("ID") != null ){
				try{
					definitionID = Integer.parseInt(requestDescriptor.request.getParameter("ID"));
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "The identifier of the definition to update is invalid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				}
				
				if( definitionID < 0 ){
					Html.addMessage(MessageType.WARNING, "The identifier of the definition to update is invalid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				}
			}
			
			//	 2.2.2 -- Update the definition
			if( requestDescriptor.request.getParameter("Compile") != null && (requestDescriptor.request.getParameter("SignatureCode") != null || requestDescriptor.request.getParameter("SignatureCode2") != null) && definitionID > -1){
				String code;
				code = requestDescriptor.request.getParameter("SignatureCode");
				
				if( code == null ){
					code = requestDescriptor.request.getParameter("SignatureCode2");
				}
				
				Definition definition = null;
				
				try{
					if( requestDescriptor.request.getParameter("Type").equalsIgnoreCase("Pattern") ){
						definition = PatternDefinition.parse(code);
					}
					else if( requestDescriptor.request.getParameter("Type").equalsIgnoreCase("Script") ){
						definition = ScriptDefinition.parse(code);
					}
				}
				catch(InvalidDefinitionException e)
				{
					Html.addMessage(MessageType.WARNING, "The definition given is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED);
				}
				catch(UnpurposedDefinitionException e)
				{
					Html.addMessage(MessageType.WARNING, "The definition given is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				}
				
				
				ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
				
				try {
					definitionSet.updateDefinition(requestDescriptor.sessionIdentifier, definition, definitionID);
					
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to create new rules", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				} catch (DisallowedOperationException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "The definition was updated successfully", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			
			return new ActionDescriptor(ActionDescriptor.OP_UPDATE);
		}
		
		// 3 -- User is deleting a definition
		else if( action.matches( "Delete" ) ){
			
			// 3.1 -- User has cancelled the editing of a definition (opened the form, then pressed 'cancel')
			if( requestDescriptor.request.getParameter("Cancel") != null ){
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
			
			// 3.2 -- Delete the definition
			if( requestDescriptor.request.getParameter("ID") != null ){
				ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
				
				int ruleId;
				
				try{
					ruleId = Integer.parseInt(requestDescriptor.request.getParameter("ID"));
				}
				catch( NumberFormatException e){
					throw new InvalidHtmlParameterException("Rule ID Invalid", "The rule ID given is not a valid integer", "/Definitions");
				}
				
				try {
					definitionSet.removeDefinition(requestDescriptor.sessionIdentifier, ruleId);
				}
				catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to delete rules", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
				}
				catch (DisallowedOperationException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Definition successfully deleted", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_SUCCESS );
			}
			else {
				Html.addMessage(MessageType.INFORMATIONAL, "No definition ID given, definition not deleted", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED );
			}
			
		}
		
		// 4 -- Export the definitions as XML
		else if( action.matches( "Export" ) ){
			ApiDefinitionSet definitionSet = new ApiDefinitionSet(Application.getApplication());
			
			try{
				String xml = definitionSet.exportDefinitions(requestDescriptor.sessionIdentifier, true);
				requestDescriptor.response.setContentType("text/xml");
				
				return new ActionDescriptor(OP_EXPORT, xml);
				
			}
			catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to download the rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
			}
			
		}
		
		// 5 -- Import the definitions (from XML)
		else if( action.matches( "Import" ) ){
			//Determine if a multipart request was made
			boolean isMultipart = ServletFileUpload.isMultipartContent(requestDescriptor.request);
			
			if( isMultipart ){
				ServletFileUpload upload = new ServletFileUpload();
				
				try{
					FileItemIterator iter = upload.getItemIterator(requestDescriptor.request);
					
					while ( iter.hasNext() ) {
					    FileItemStream item = iter.next();
					    
					    if( item.getFieldName().equalsIgnoreCase("DefinitionsFile") ){
					    	InputStream stream = item.openStream();
					    	
					    	String rulesXml = getString( stream, 16000000 );
					    	
					    	ApiDefinitionSet definitionSet;
					    	
					    	try{
					    		definitionSet = new ApiDefinitionSet(Application.getApplication(), true);
					    		definitionSet.importDefinitions(requestDescriptor.sessionIdentifier, rulesXml, false);
					    	}
					    	catch(DefinitionSetLoadException e){
					    		Html.addMessage(MessageType.WARNING, "Definition file is not valid", requestDescriptor.userId.longValue());
					    		return new ActionDescriptor(OP_IMPORT_FAILED );
					    	}
					    	catch(DefinitionArchiveException e){
					    		Html.addMessage(MessageType.WARNING, "Definition file is not valid", requestDescriptor.userId.longValue());
					    		return new ActionDescriptor(OP_IMPORT_FAILED );
					    	}
					    	
					    	Html.addMessage(MessageType.INFORMATIONAL, "Definition file successfully imported", requestDescriptor.userId.longValue());
					    	return new ActionDescriptor(OP_IMPORT_SUCCESS );
					    }
					   
					}
				}
				catch(FileUploadException e){
					Html.addMessage(MessageType.WARNING, "Definition file upload failed", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_IMPORT_FAILED );
				}
				catch(IOException e){
					Html.addMessage(MessageType.WARNING, "Definition file upload failed", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_IMPORT_FAILED );
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update the definitions", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_IMPORT_FAILED );
				}
				
				//Html.addMessage(MessageType.INFORMATIONAL, "Definition file successfully imported", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_IMPORT_SUCCESS );
			}
			else{
				return new ActionDescriptor(OP_IMPORT);
			}
		}
		
		// 6 -- Update the definitions from the server
		else if( action.matches( "Update" ) ){
			ApiDefinitionSet definitionSet;
			definitionSet = new ApiDefinitionSet(Application.getApplication(), true);
			
			try{
				definitionSet.updateDefinitionsAsWorker(requestDescriptor.sessionIdentifier, true);
				return new ActionDescriptor( OP_DEFINITIONS_UPDATE_IN_PROCESS );
			} catch( DuplicateEntryException e ){
				Html.addMessage(MessageType.WARNING, "Definitions are already being updated", requestDescriptor.userId);
    			return new ActionDescriptor( OP_DEFINITIONS_UPDATE_FAILED );
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to update definitions", requestDescriptor.userId);
    			return new ActionDescriptor( OP_DEFINITIONS_UPDATE_FAILED );
			}
			
		}
		else if( action.matches( "Update2" ) ){
			ApiDefinitionSet definitionSet;
			DefinitionVersionID versionID = null;
			
	    	try{
	    		definitionSet = new ApiDefinitionSet(Application.getApplication(), true);
	    		versionID = definitionSet.updateDefinitions(requestDescriptor.sessionIdentifier);
	    	} catch(DefinitionUpdateFailedException e){
	    		Html.addMessage(MessageType.WARNING, "The definitions were not updated successfully", requestDescriptor.userId);
	    		return new ActionDescriptor( OP_DEFINITIONS_UPDATE_FAILED);
	    	} catch (InsufficientPermissionException e) {
	    		Html.addMessage(MessageType.WARNING, "You do not have permission to update definitions", requestDescriptor.userId);
	    		return new ActionDescriptor( OP_DEFINITIONS_UPDATE_FAILED );
			}
	    	
	    	if( versionID == null ){
	    		Html.addMessage(MessageType.WARNING, "The definitions were not updated successfully", requestDescriptor.userId);
	    		return new ActionDescriptor(OP_DEFINITIONS_UPDATE_FAILED );
	    	}
	    	else{
	    		Html.addMessage(MessageType.INFORMATIONAL, "Definitions successfully updated", requestDescriptor.userId);
	    		return new ActionDescriptor(OP_DEFINITIONS_UPDATE_SUCCESS );
	    	}
		}
		
		//Default action: no operation specified
		else{
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
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

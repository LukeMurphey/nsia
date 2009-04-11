package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.scanRules.DefinitionError;
import net.lukeMurphey.nsia.scanRules.DefinitionErrorList;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;

public class HtmlDefinitionStatus extends HtmlContentProvider {

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		
		StringBuffer body = new StringBuffer();
		
		// 0 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor );
		
		
		// 1 -- Get the view of definitions with errors
		ApiScannerController controller = new ApiScannerController(Application.getApplication());
		DefinitionErrorList list = controller.getDefinitionsErrorList(requestDescriptor.sessionIdentifier);
		DefinitionError[] definitionsWithErrors = list.getErrorsList();
		
		if( definitionsWithErrors.length > 0 ){
			body.append("<table style=\"width: 100%\" class=\"DataTable\"><thead><tr><td>Definition Name</td><td>Version</td><td>ID</td><td>Error</td><td>First Noted</td><td>Last Noted</td></tr></thead>");
			body.append( "<tbody>" );
			
			for( int c = 0; c < definitionsWithErrors.length; c++ ){
				body.append( "<tr>" );
				
				body.append( "<td><table><tr><td><img src=\"/16_script\"</td><td>" );
				body.append( "<a href=\"/Definitions?Action=Edit&ID=" + definitionsWithErrors[c].getLocalDefinitionID() + "\">" );
				body.append( definitionsWithErrors[c].getDefinitionName() );
				body.append( "</a></td></tr></table></td>" );
				
				body.append( "<td>" );
				body.append( definitionsWithErrors[c].getDefinitionVersion() );
				body.append( "</td>" );
				
				body.append( "<td>" );
				body.append( definitionsWithErrors[c].getDefinitionID() );
				body.append( "</td>" );
				
				body.append( "<td>" );
				body.append( definitionsWithErrors[c].getErrorName() );
				body.append( "</td>" );
				
				body.append( "<td>" );
				body.append( definitionsWithErrors[c].getDateFirstOccurred() );
				body.append( "</td>" );
				
				body.append( "<td>" );
				body.append( definitionsWithErrors[c].getDateLastOccurred() );
				body.append( "</td>" );
				
				body.append( "</tr>" );
			}
			
			body.append( "</tbody>" );
			body.append( "</table>" );
		}
		else{
			body.append( Html.getDialog("No errors have been observed in the current set of definitions.<p/><a href=\"/Definitions\">[View All Definitions]</a>", "No Errors", "/32_Information.png", false) );
		}
		
		// 2 -- Perform the relevant operation
		if( actionDesc.result == ActionDescriptor.OP_ADD  ){
			
		}
		

		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Definitions", "/Definitions");
		navPath.addPathEntry( "Errors", "/DefinitionErrors");
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
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		
	}
	
}

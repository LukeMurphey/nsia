package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyManagement;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ExceptionDeleteView extends View {

	public static final String VIEW_NAME = "exception_delete";
	
	public ExceptionDeleteView() {
		super("Exception/Delete", VIEW_NAME, Pattern.compile("[0-9]?"));
	}
	
	public static String getURL( int exceptionID ) throws URLInvalidException{
		ExceptionDeleteView view = new ExceptionDeleteView();
		return view.createURL(exceptionID);
	}
	
	private int deleteExceptions( RequestContext context, Vector<Integer> exceptions ) throws SQLException, NoDatabaseConnectionException, NotFoundException, MalformedURLException, ViewFailedException, InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 1 -- Delete the policies
		int exceptionsDeleted = 0;
		DefinitionPolicyManagement policyManagement = new DefinitionPolicyManagement(Application.getApplication());
		SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
		SiteGroupDescriptor siteGroup = null;
		
		for (int exceptionID : exceptions) {
			
			// Get the rule associated with the exception
			DefinitionPolicyDescriptor policy = policyManagement.getPolicy(exceptionID);
			int siteGroupID = policy.getSiteGroupID();
			
			// Get the site group associated with the rule
			if( siteGroupID <= 0 ){
				int ruleID = policy.getPolicyRuleID();
				siteGroupID = ScanRule.getSiteGroupForRule(ruleID);
			}
			
			// Ensure the user has permission to edit the site-group
			if( siteGroupID <= 0 ){
				throw new ViewFailedException("Could not find the site-group associated with the definition policy " + exceptionID);
			}
			else{
				try {
					// Get the site-group if it was not already loaded
					if( siteGroup == null || siteGroup.getGroupId() != siteGroupID ){
							siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
					}
					
					//Check rights
					Shortcuts.checkDelete(context.getSessionInfo(), siteGroup.getObjectId(), "Delete definition policy ID " + exceptionID + " from site-group ID " + siteGroup.getGroupId() + "( " + siteGroup.getGroupName() + ")");
					
				} catch (InputValidationException e) {
					throw new ViewFailedException(e);
				}
			}
			
			policyManagement.deletePolicy(exceptionID);
			exceptionsDeleted++;
		}
		
		context.addMessage(exceptionsDeleted + " exceptions have been deleted", MessageSeverity.SUCCESS);
		
		return exceptionsDeleted;
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the exceptions to delete
		Vector<Integer> exceptionsToDelete = new Vector<Integer>();
		
		String[] identifiers = request.getParameterValues("ExceptionID");
		
		// Convert the exception identifiers to integers
		for (String stringID : identifiers) {
			
			try{
				exceptionsToDelete.add( new Integer( stringID ) );
			}
			catch(NumberFormatException e){
				//At least one the IDs was invalid
			}
		}
		
		// 2 -- Get the rule ID
		int ruleID = Integer.valueOf( args[0] );
		
		// 3 -- Delete the exceptions
		try{
			deleteExceptions(context, exceptionsToDelete);
		}
		catch( NotFoundException e ){
			Dialog.getDialog(response, context, data, "Exception could not be deleted because it could not be found", "Exception Not Found", DialogType.WARNING);
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InsufficientPermissionException e) {
			context.addMessage("You do not have permission to delete exceptions for this site-group", MessageSeverity.WARNING);
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		}
		
		response.sendRedirect( ExceptionListView.getURL(ruleID) );
		return true;
	}

}

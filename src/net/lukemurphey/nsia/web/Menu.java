package net.lukemurphey.nsia.web;

import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
import net.lukemurphey.nsia.GroupManagement.State;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.UserManagement.AccountStatus;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.web.views.AccessControlView;
import net.lukemurphey.nsia.web.views.ActionEditView;
import net.lukemurphey.nsia.web.views.ActionsListView;
import net.lukemurphey.nsia.web.views.BackupView;
import net.lukemurphey.nsia.web.views.DefinitionDeleteView;
import net.lukemurphey.nsia.web.views.DefinitionEntryView;
import net.lukemurphey.nsia.web.views.DefinitionPolicyView;
import net.lukemurphey.nsia.web.views.DefinitionsExportView;
import net.lukemurphey.nsia.web.views.DefinitionsImportView;
import net.lukemurphey.nsia.web.views.DefinitionsUpdateView;
import net.lukemurphey.nsia.web.views.DefinitionsView;
import net.lukemurphey.nsia.web.views.DefragmentIndexesView;
import net.lukemurphey.nsia.web.views.EventLogView;
import net.lukemurphey.nsia.web.views.ExceptionListView;
import net.lukemurphey.nsia.web.views.GroupDeleteView;
import net.lukemurphey.nsia.web.views.GroupDisableView;
import net.lukemurphey.nsia.web.views.GroupEditView;
import net.lukemurphey.nsia.web.views.GroupEnableView;
import net.lukemurphey.nsia.web.views.GroupListView;
import net.lukemurphey.nsia.web.views.RightsEditView;
import net.lukemurphey.nsia.web.views.RuleEditView;
import net.lukemurphey.nsia.web.views.ScanResultHistoryView;
import net.lukemurphey.nsia.web.views.ScannerStartView;
import net.lukemurphey.nsia.web.views.ScannerStopView;
import net.lukemurphey.nsia.web.views.ShutdownView;
import net.lukemurphey.nsia.web.views.SiteGroupDeleteView;
import net.lukemurphey.nsia.web.views.SiteGroupDisableView;
import net.lukemurphey.nsia.web.views.SiteGroupEditView;
import net.lukemurphey.nsia.web.views.SiteGroupEnableView;
import net.lukemurphey.nsia.web.views.SystemConfigurationView;
import net.lukemurphey.nsia.web.views.SystemStatusView;
import net.lukemurphey.nsia.web.views.UserDisableView;
import net.lukemurphey.nsia.web.views.UserEditView;
import net.lukemurphey.nsia.web.views.UserEnableView;
import net.lukemurphey.nsia.web.views.UserPasswordUpdateView;
import net.lukemurphey.nsia.web.views.UserSessionsView;
import net.lukemurphey.nsia.web.views.UsersView;

public class Menu {

	private static Link[] toArray( Vector<Link> menus ){
		Link[] menu_array = new Link[menus.size()];
		menus.toArray(menu_array);
		return menu_array;
	}
	
	public static Vector<Link> getSystemMenuItems( RequestContext context ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("System Administration") );
		menu.add( new Link("System Status", SystemStatusView.getURL() ) );
		menu.add( new Link("System Configuration", SystemConfigurationView.getURL()) );
		menu.add( new Link("Event Logs", EventLogView.getURL()) );
		menu.add( new Link("Shutdown System", ShutdownView.getURL()) );
		menu.add( new Link("Create Backup", BackupView.getURL()) );
		menu.add( new Link("Defragment Indexes", DefragmentIndexesView.getURL()) );
		
		menu.add( new Link("Scanning Engine") );
		if( Application.getApplication().getScannerController().scanningEnabled() ){
			menu.add( new Link("Stop Scanner", ScannerStopView.getURL()) );
		}
		else{
			menu.add( new Link("Start Scanner", ScannerStartView.getURL()) );
		}
		menu.add( new Link("View Definitions", DefinitionsView.getURL()) );
		
		return menu;
	}
	
	public static Link[] getSystemMenu( RequestContext context ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.addAll(getSystemMenuItems(context));
		
		menu.addAll(getSiteGroupMenuItems(context, null));
		
		menu.addAll(getUserMenuItems(context, null));
		
		menu.addAll(getGroupMenuItems(context, null));
		
		return toArray(menu);
	}
	
	public static Vector<Link> getSiteGroupMenuItems( RequestContext context ) throws URLInvalidException{
		return getSiteGroupMenuItems(context, null);
	}
	
	public static Vector<Link> getSiteGroupMenuItems( RequestContext context, SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.add( new Link("Site Groups") );
		menu.add( new Link("Add Site Group", SiteGroupEditView.getURL() ) );
		
		if( siteGroup != null ){
			menu.add( new Link("Edit Site Group", SiteGroupEditView.getURL(siteGroup)) );
			menu.add( new Link("Edit ACLs", AccessControlView.getURL(siteGroup.getObjectId()), new Link.Attribute("onclick", "w=window.open('" + AccessControlView.getURL(siteGroup.getObjectId()) + "', 'AccessControl', 'height=400,width=780,screenX=' + (screen.availWidth - 700)/2 + ',screenY=' + (screen.availHeight - 300)/2 + ',scrollbars=yes,resizable=yes,toolbar=no');return false") ) );
			menu.add( new Link("Edit Scan Policy", DefinitionPolicyView.getURL(siteGroup) ));
			menu.add( new Link("Delete Site Group", SiteGroupDeleteView.getURL(siteGroup), new Link.Attribute("onclick", "return confirm('Are you sure you want to delete this Site-Group?')") ) ); // , new Link.Attribute("onclick", "\"$('#delete_dialog').dialog('open');return false;\"")
			
			if( siteGroup.isEnabled() ){
				menu.add( new Link("Disable Site Group", SiteGroupDisableView.getURL(siteGroup) ) );
			}
			else{
				menu.add( new Link("Enable Site Group", SiteGroupEnableView.getURL(siteGroup)) );
			}
		}
		
		return menu;
	}
	
	public static Vector<Link> getGroupMenuItems( RequestContext context ) throws URLInvalidException{
		return getGroupMenuItems(context, null);
	}
	
	public static Vector<Link> getGroupMenuItems( RequestContext context, GroupDescriptor group ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.add( new Link("Group Management") );
		menu.add( new Link("List Groups", GroupListView.getURL() ) );
		menu.add( new Link("Add New Group", GroupEditView.getURL() ) );
		
		if( group != null ){
			menu.add( new Link("Edit Group", GroupEditView.getURL(group) ) );
			
			if(  group.getGroupState() == State.INACTIVE ){
				menu.add( new Link("Enable Group", GroupEnableView.getURL(group) ) );
			}
			else if(group != null){
				menu.add( new Link("Disable Group", GroupDisableView.getURL(group) ) );
			}
			
			menu.add( new Link("Edit Rights", RightsEditView.getURL(group) ) );
			menu.add( new Link("Delete Group", GroupDeleteView.getURL(group), new Link.Attribute("onclick", "return confirm('Are you sure you want to delete this group?')") ) );
		}
		
		return menu;
	}
	
	public static Vector<Link> getUserMenuItems( RequestContext context ) throws URLInvalidException{
		return getUserMenuItems(context, null);
	}
	
	public static Vector<Link> getUserMenuItems( RequestContext context, UserDescriptor user ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("User Management") );
		menu.add( new Link("List Users", UsersView.getURL()) );
		menu.add( new Link("Add New User", UserEditView.getURL()) );
		menu.add( new Link("View Logged in Users", UserSessionsView.getURL()) );
		
		if( user != null ){
			if(  user.getAccountStatus() == AccountStatus.DISABLED ){
				menu.add( new Link("Enable User", UserEnableView.getURL(user) ) );
			}
			else if(user != null){
				menu.add( new Link("Disable User", UserDisableView.getURL(user) ) );
			}
				
			//menu.add( new Link("Delete User", "") );
			menu.add( new Link("Manage Rights", RightsEditView.getURL(user)) );
			menu.add( new Link("Update Password", UserPasswordUpdateView.getURL(user)) );
		}
		
		return menu;
	}
		
	public static Vector<Link> getResponseActionsMenuItems( RequestContext context, SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		if( siteGroup != null ){
			menu.add( new Link("Incident Response") );
			menu.add( new Link("Add New Action", ActionEditView.getURL() + "?SiteGroupID=" + siteGroup.getGroupId() ) );
			menu.add( new Link("List Actions", ActionsListView.getURL(siteGroup.getGroupId()) ) );
		}
		return menu;
	}
		
	public static Link[] getSiteGroupMenu( RequestContext context, SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.addAll(getSiteGroupMenuItems(context, siteGroup));
		menu.addAll(getResponseActionsMenuItems(context, siteGroup));
		return toArray(menu);
	}
	
	public static Link[] getUserMenu( RequestContext context ) throws URLInvalidException{
		return getUserMenu( context, null );
	}
	
	public static Link[] getUserMenu( RequestContext context, UserDescriptor user ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.addAll( getSiteGroupMenuItems(context) );
		menu.addAll( getUserMenuItems(context, user) );
		menu.addAll( getGroupMenuItems(context) );
		
		return toArray(menu);
	}
	
	public static Vector<Link> getScanRuleMenuItems( RequestContext context, SiteGroupDescriptor siteGroup, long ruleID ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.add( new Link("Scan Rule") );
		menu.add( new Link("View Exceptions", ExceptionListView.getURL(ruleID)) );
		menu.add( new Link("Scan History", ScanResultHistoryView.getURL(ruleID)) );
		menu.add( new Link("Edit Rule", RuleEditView.getURL(ruleID)) );
		
		if( siteGroup != null ){
			menu.add( new Link("New Rule", RuleEditView.getURL(siteGroup)) );
		}
		
		return menu;
	}
	
	public static Link[] getScanRuleMenu( RequestContext context, SiteGroupDescriptor siteGroup, long ruleID ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.addAll( getSiteGroupMenuItems(context, siteGroup) );
		menu.addAll( getScanRuleMenuItems(context, siteGroup, ruleID ) );
		menu.addAll( getResponseActionsMenuItems(context, siteGroup) ) ;
		
		return toArray(menu);
	}
	
	public static Link[] getScanResultMenu( RequestContext context, int scanResultID ) throws URLInvalidException{
		return getScanResultMenu(context, scanResultID);
	}
	
	public static Link[] getScanResultMenu( RequestContext context, int scanResultID, SiteGroupDescriptor siteGroup, ScanRule rule ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.addAll( getSiteGroupMenuItems(context, null) );
		menu.addAll( getUserMenuItems(context, null) );
		menu.addAll( getGroupMenuItems(context, null) );
		if( siteGroup != null && rule != null ){
			menu.addAll( getScanRuleMenuItems(context, siteGroup, rule.getRuleId()) );
		}
		
		return toArray(menu);
	}
	
	public static Vector<Link> getDefinitionMenuItems( RequestContext context, Definition definition, SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		
		menu.add( new Link("Definitions") );
		if( definition != null && definition.isOfficial() == false ){
			//menu.add( new Link("Delete Definition", DefinitionDeleteView.getURL(definition.getID()), new Link.Attribute("onclick", "return confirm('Are you sure you want to delete this definition?')") ) );
			menu.add( new Link("Delete Definition", DefinitionDeleteView.getURL(definition.getID()) ) );
		}
		menu.add( new Link("Update Definitions", DefinitionsUpdateView.getURL() ) );
		menu.add( new Link("Create New Definition", DefinitionEntryView.getURL() ));
		menu.add( new Link("Import Definitions", DefinitionsImportView.getURL()) );
		menu.add( new Link("Export Custom Definitions", DefinitionsExportView.getURL() ));
		menu.add( new Link("Edit Default Policy", DefinitionPolicyView.getURL() ));
		
		return menu;
	}
	
	public static Link[] getDefinitionMenu( RequestContext context ) throws URLInvalidException{
		return getDefinitionMenu(context, null);
	}
	
	public static Link[] getDefinitionMenu( RequestContext context, Definition definition ) throws URLInvalidException{
		
		Vector<Link> menu = new Vector<Link>();
		//menu.addAll(getSystemMenuItems(context));
		menu.addAll(getDefinitionMenuItems(context, definition, null));
		
		return toArray(menu);
	}
	
	public static Link[] getGenericMenu(RequestContext context ) throws URLInvalidException{
		Vector<Link> menu = new Vector<Link>();
		menu.addAll(getSiteGroupMenuItems(context));
		menu.addAll(getUserMenuItems(context));
		menu.addAll(getGroupMenuItems(context));
		
		return toArray(menu);
	}
	
}

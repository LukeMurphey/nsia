package net.lukemurphey.nsia.web;

import net.lukemurphey.nsia.web.views.BackupView;
import net.lukemurphey.nsia.web.views.DashboardStatusPanel;
import net.lukemurphey.nsia.web.views.DefinitionDeleteView;
import net.lukemurphey.nsia.web.views.DefinitionEntryView;
import net.lukemurphey.nsia.web.views.DefinitionErrorsView;
import net.lukemurphey.nsia.web.views.DefinitionPolicyView;
import net.lukemurphey.nsia.web.views.DefinitionsExportView;
import net.lukemurphey.nsia.web.views.DefinitionsImportView;
import net.lukemurphey.nsia.web.views.DefinitionsView;
import net.lukemurphey.nsia.web.views.DefragmentIndexesView;
import net.lukemurphey.nsia.web.views.EventLogEntryView;
import net.lukemurphey.nsia.web.views.EventLogView;
import net.lukemurphey.nsia.web.views.LicenseView;
import net.lukemurphey.nsia.web.views.LoginView;
import net.lukemurphey.nsia.web.views.LogoutView;
import net.lukemurphey.nsia.web.views.MainDashboardView;
import net.lukemurphey.nsia.web.views.ShutdownView;
import net.lukemurphey.nsia.web.views.ScannerStartView;
import net.lukemurphey.nsia.web.views.ScannerStopView;
import net.lukemurphey.nsia.web.views.SiteGroupDeleteView;
import net.lukemurphey.nsia.web.views.SiteGroupDisableView;
import net.lukemurphey.nsia.web.views.SiteGroupEditView;
import net.lukemurphey.nsia.web.views.SiteGroupEnableView;
import net.lukemurphey.nsia.web.views.SiteGroupView;
import net.lukemurphey.nsia.web.views.SystemConfigurationView;
import net.lukemurphey.nsia.web.views.SystemStatusView;
import net.lukemurphey.nsia.web.views.TaskListView;
import net.lukemurphey.nsia.web.views.TaskStopView;
import net.lukemurphey.nsia.web.views.DefinitionsUpdateView;
import net.lukemurphey.nsia.web.views.UserDisableView;
import net.lukemurphey.nsia.web.views.UserEnableView;
import net.lukemurphey.nsia.web.views.UserGroupMembershipEditView;
import net.lukemurphey.nsia.web.views.UserView;
import net.lukemurphey.nsia.web.views.UsersView;

public class StandardViewList {

	private static ViewList view_list = null;
	
	private static synchronized ViewList populateViews(){
		
		if( view_list == null ){
			view_list = new ViewList();
			
			try {
				view_list.registerView( new LoginView() );
				view_list.registerView( new LogoutView() );
				view_list.registerView( new MainDashboardView() );
				view_list.registerView( new DashboardStatusPanel() );
				view_list.registerView( new DefragmentIndexesView() );
				view_list.registerView( new ScannerStopView() );
				view_list.registerView( new ScannerStartView() );
				view_list.registerView( new SystemStatusView() );
				view_list.registerView( new SystemConfigurationView() );
				view_list.registerView( new EventLogView() );
				view_list.registerView( new EventLogEntryView() );
				view_list.registerView( new ShutdownView() );
				view_list.registerView( new BackupView() );
				view_list.registerView( new TaskListView() );
				view_list.registerView( new TaskStopView() );
				view_list.registerView( new DefinitionPolicyView() );
				view_list.registerView( new DefinitionErrorsView() );
				view_list.registerView( new DefinitionsUpdateView() );
				view_list.registerView( new DefinitionsView() );
				view_list.registerView( new DefinitionEntryView() );
				view_list.registerView( new DefinitionDeleteView() );
				view_list.registerView( new DefinitionsExportView() );
				view_list.registerView( new DefinitionsImportView() );
				view_list.registerView( new LicenseView() );
				view_list.registerView( new SiteGroupEditView() );
				view_list.registerView( new SiteGroupView() );
				view_list.registerView( new SiteGroupDeleteView() );
				view_list.registerView( new SiteGroupDisableView() );
				view_list.registerView( new SiteGroupEnableView() );
				view_list.registerView( new UserEnableView() );
				view_list.registerView( new UserDisableView() );
				view_list.registerView( new UsersView() );
				view_list.registerView( new UserView() );
				view_list.registerView( new UserGroupMembershipEditView() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return view_list;
	}
	
	public static View getView( String name ){
		return view_list.findView(name);
	}
	
	public static String getURL( String name, Object... args ) throws URLInvalidException, ViewNotFoundException{
		View view = view_list.findView(name);
		
		if( view != null ){
			return view.createURL(args);
		}
		else{
			throw new ViewNotFoundException(name);
		}
	}
	
	public static ViewList getViewList(){
		
		if( view_list == null ){
			return populateViews();
		}
		else{
			return view_list;
		}
	}
	
}

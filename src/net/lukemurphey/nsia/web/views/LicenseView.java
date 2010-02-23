package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationConfiguration;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseStatus;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class LicenseView extends View {

	public static final String VIEW_NAME = "license";
	
	public LicenseView() {
		super("License", VIEW_NAME);
	}
	
	public static String getURL() throws URLInvalidException{
		LicenseView view = new LicenseView();
		return view.createURL();
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		try{
			// 1 -- Check rights
			//checkRight( context.getSessionInfo(), "System.Configuration.View");
			
			// 2 -- Get the license
			ApplicationConfiguration config = Application.getApplication().getApplicationConfiguration();
			LicenseDescriptor license = null;
			license = config.getLicense();
			String licenseKey;
			
			// 3 -- Apply license if requested
			if( request.getParameter("LicenseKey") != null ){
				licenseKey = request.getParameter("LicenseKey");
				
				// Set new key if the operation is a POST and a valid key was provided
				if( "POST".equalsIgnoreCase( request.getMethod() ) && licenseKey != null ){
					config.setLicenseKey(licenseKey);
					license = config.getLicense(false); //Get the updated license status
				}
			}
			else if(license == null){
				licenseKey = "";
			}
			else if( license.getKey() != null ){
				licenseKey = license.getKey();
			}
			else{
				licenseKey = "";
			}
			
			// 4 -- Get the license and the status of the license
			
			//	 4.1 -- Insert all of the possible license statuses
			data.put("ACTIVE", LicenseStatus.ACTIVE);
			data.put("DEMO", LicenseStatus.DEMO);
			data.put("DISABLED", LicenseStatus.DISABLED);
			data.put("EXPIRED", LicenseStatus.EXPIRED);
			data.put("ILLEGAL", LicenseStatus.ILLEGAL);
			data.put("UNLICENSED", LicenseStatus.UNLICENSED);
			data.put("UNVALIDATED", LicenseStatus.UNVALIDATED);
			
			//	 4.2 -- Get the license
			data.put("license_key", licenseKey);
			data.put("license", license);
			
			//	 4.3 -- Determine if the license check completed
			data.put("license_check_completed", config.licenseKeyCheckCompleted());
			
			
			// 5 -- Create the menu and breadcrumbs
			
			//	 5.1 -- Breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add( new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add( new Link("System Status", StandardViewList.getURL("system_status")) );
			breadcrumbs.add( new Link("License Management", createURL()) );
			
			data.put("breadcrumbs", breadcrumbs);
			
			//	 5.2 -- Menu
			data.put("menu", Menu.getSystemMenu(context));
			data.put("title", "License");
			
			//Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			TemplateLoader.renderToResponse("LicenseView.ftl", data, response);
			
			return true;
		}
		catch( InputValidationException e ){
			throw new ViewFailedException(e);
		}
		catch( SQLException e ){
			throw new ViewFailedException(e);
		}
		catch( NoDatabaseConnectionException e ){
			throw new ViewFailedException(e);
		}
	}

}

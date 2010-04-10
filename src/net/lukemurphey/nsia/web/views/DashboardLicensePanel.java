package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseStatus;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardLicensePanel extends View {

	public DashboardLicensePanel() {
		super("DashboardPanel/License", "dashboard_panel_license");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		String panel = getPanel(request, data, Application.getApplication());
		
		if( panel != null ){
			response.getOutputStream().print(panel);
		}
		
		return true;
	}
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
			
		// 1 -- Get the license
		LicenseDescriptor license;
		
		try {
			license = app.getApplicationConfiguration().getLicense(true);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Add the status codes
		data.put("ACTIVE", LicenseStatus.ACTIVE);
		data.put("DEMO", LicenseStatus.DEMO);
		data.put("DISABLED", LicenseStatus.DISABLED);
		data.put("EXPIRED", LicenseStatus.EXPIRED);
		data.put("ILLEGAL", LicenseStatus.ILLEGAL);
		data.put("UNLICENSED", LicenseStatus.UNLICENSED);
		data.put("UNVALIDATED", LicenseStatus.UNVALIDATED);
		
		// 2 -- Determine if the license was validated yet
		boolean license_unvalidated = (license == null || license.getStatus() == null || license.getStatus() == LicenseStatus.UNVALIDATED);
		
		// 3 -- Post the panel only if a license issue was noted
		if( app.getApplicationConfiguration().licenseKeyCheckCompleted() && (license == null || license.isValid() == false )){
			
			data.put("license_check_completed", !license_unvalidated); 
			data.put("license", license);
				
			return TemplateLoader.renderToString("DashboardLicenseWarning.ftl", data);
		}
		else{
			return null;
		}
		

	}

}

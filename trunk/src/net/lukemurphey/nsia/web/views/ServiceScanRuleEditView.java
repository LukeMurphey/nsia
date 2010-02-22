package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.scan.LineParseException;
import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ServiceScanRule;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.Field;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.HostAddressValidator;
import net.lukemurphey.nsia.web.forms.IntegerValidator;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class ServiceScanRuleEditView extends View {

	public static final String VIEW_NAME = "rule_editor_service_scan";

	public ServiceScanRuleEditView() {
		super("Rule", VIEW_NAME, Pattern.compile("New|Edit", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}
	
	public static String getURL( int ruleID ) throws URLInvalidException{
		ServiceScanRuleEditView view = new ServiceScanRuleEditView();
		return view.createURL("Edit", ruleID );
	}
	
	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		ServiceScanRuleEditView view = new ServiceScanRuleEditView();
		return view.createURL("New" ) + "?SiteGroupID=" + siteGroup.getGroupId();
	}

	/**
	 * Get a form that can validate the rule.
	 * @return
	 */
	private Form getRuleForm( ){
		Form form = new Form();

		form.addField( new Field("StartAddresses") );
		form.addField( new Field("ScanFrequencyUnits", new IntegerValidator(1,86400)) );
		form.addField( new Field("ScanFrequencyValue", new IntegerValidator(1,1000000)) );
		form.addField( new Field("Server", new HostAddressValidator()) );
		form.addField( new Field("PortsToScan") );
		form.addField( new Field("PortsExpectedOpen") );

		return form;
	}

	private boolean performActions(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data, ServiceScanRule rule) throws ViewFailedException, URLInvalidException{

		Form form = getRuleForm();

		FieldErrors errors = form.validate(request);

		if( errors.size() > 0 ){
			data.put("form_errors", errors);
		}
		else{
			String address = request.getParameter("Server");
			int scanFreqUnits = Integer.valueOf( request.getParameter("ScanFrequencyUnits"));
			int scanFreqValue = Integer.valueOf( request.getParameter("ScanFrequencyValue"));
			int scanFrequency = scanFreqUnits * scanFreqValue;

			// Get the ports expected open
			String portsExpectedOpenStr = null;
			if( request.getParameter("PortsExpectedOpen") != null ){
				portsExpectedOpenStr = request.getParameter("PortsExpectedOpen");
			}
			else if( request.getParameter("PortsExpectedOpen2") != null ){
				portsExpectedOpenStr = request.getParameter("PortsExpectedOpen2");
			}
			
			NetworkPortRange[] portsExpectedOpen;
			
			try{
				portsExpectedOpen = NetworkPortRange.parseRange(portsExpectedOpenStr);
			}
			catch(LineParseException e){
				errors = new FieldErrors();
				errors.put(new FieldError("PortsExpectedOpen", "", "The list of ports expected open is invalid"));
				data.put("form_errors", errors);
				return false;
			}
			
			// Get the ports to scan
			String portsToScanStr = null;
			if( request.getParameter("PortsToScan") != null ){
				portsToScanStr = request.getParameter("PortsToScan");
			}
			else if( request.getParameter("PortsToScan2") != null ){
				portsToScanStr = request.getParameter("PortsToScan2");
			}
			
			if( portsToScanStr == null ){
				errors = new FieldErrors();
				errors.put(new FieldError("PortsToScan", "", "The list of ports to scan were not provided"));
				data.put("form_errors", errors);
				return false;
			}
			
			NetworkPortRange[] portsToScan = null;
			
			try{
				portsToScan = NetworkPortRange.parseRange(portsToScanStr);
			}
			catch(LineParseException e){
				errors = new FieldErrors();
				errors.put(new FieldError("PortsToScan", "", "The list of ports to scan is invalid"));
				data.put("form_errors", errors);
				return false;
			}
			
			if( portsToScan.length == 0 ){
				errors = new FieldErrors();
				errors.put(new FieldError("PortsToScan", "", "No ports to scan were provided"));
				data.put("form_errors", errors);
				return false;
			}
			
			boolean isNewRule = false;

			if( rule == null ){
				rule = new ServiceScanRule(Application.getApplication(), address, portsExpectedOpen, portsToScan );
				rule.setScanFrequency(scanFrequency);
				isNewRule = true;
			}
			else{
				rule.setScanFrequency(scanFrequency);
				rule.setPortsExpectedOpen(portsExpectedOpen);
				rule.setPortsToScan(portsToScan);
				rule.setServerAddress(address);
			}

			try {
				if( isNewRule ){
					int siteGroupID = Integer.valueOf( request.getParameter("SiteGroupID") );
					rule.saveNewRuleToDatabase(siteGroupID);
					context.addMessage("Rule successfully created", MessageSeverity.SUCCESS);
					response.sendRedirect( SiteGroupView.getURL(siteGroupID) );
					return true;
				}
				else{
					rule.saveToDatabase();
					context.addMessage("Rule successfully updated", MessageSeverity.SUCCESS);
					response.sendRedirect( SiteGroupView.getURL( ScanRule.getSiteGroupForRule(rule.getRuleId())) );
					return true;
				}


			} catch (IllegalStateException e) {
				throw new ViewFailedException(e);
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (IOException e) {
				throw new ViewFailedException(e);
			}
		}

		return false;
	}

	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
	throws ViewFailedException, URLInvalidException, IOException,
	ViewNotFoundException {

		boolean viewHandled = false;
		ServiceScanRule rule = null;

		if( data.get("rule") != null ){
			rule = (ServiceScanRule)data.get("rule");
		}
		
		if( request.getMethod().equalsIgnoreCase("POST") ){
			viewHandled = performActions(request, response, context, args, data, rule);
		}

		if( viewHandled == false ){
			TemplateLoader.renderToResponse("ServiceRule.ftl", data, response);
		}

		return true;
	}

}

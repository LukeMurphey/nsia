package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.MaxMinCount;
import net.lukemurphey.nsia.NameIntPair;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.HttpDefinitionScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class WebDiscoveryScanResultView extends View {

	public static final String VIEW_NAME = "scan_result";
	private static int RESULTS_PER_PAGE = 30;
	
	public WebDiscoveryScanResultView() {
		super("ScanResult", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	static class SignatureMatchCount{
		
		public SignatureMatchCount( String name ){
			count = 1;
			this.name = name;
		}
		
		public String name;
		public int count;
	}
	
	static class ContentTypeCount{
		
		public ContentTypeCount( String contentType ){
			count = 1;
			this.contentType = contentType;
		}
		
		public String contentType;
		public int count;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		HttpSeekingScanResult scanResult = (HttpSeekingScanResult)data.get("scanResult");
		
		//Filter the output
		String scanRuleFilter = request.getParameter("RuleFilter");
		String contentTypeFilter = request.getParameter("ContentTypeFilter");
		
		HttpDefinitionScanResult.SignatureScanResultFilter filter;
		
		if( contentTypeFilter != null && contentTypeFilter.equalsIgnoreCase("[unknown]" )){
			filter = new HttpDefinitionScanResult.SignatureScanResultFilter("", scanRuleFilter);
		}
		else{
			filter = new HttpDefinitionScanResult.SignatureScanResultFilter(contentTypeFilter, scanRuleFilter);
		}
		
		HttpDefinitionScanResult[] findings = scanResult.getFindings();
		MaxMinCount maxMinCount = null;
		
		long firstScanResultId = -1;
		long lastScanResultId = -1;
		long startEntry = -1;
		boolean resultsBefore = false;
		
		try{
			if( request.getParameter("S") != null ){
				firstScanResultId = Long.valueOf( request.getParameter("S") );
			}
			
			if( request.getParameter("E") != null ){
				lastScanResultId = Long.valueOf( request.getParameter("E") );
			}
			
			String action = request.getParameter("Action");
			if( action != null && action.equalsIgnoreCase("Previous") ){
				startEntry = firstScanResultId;
				resultsBefore = true;
			}
			else if( action != null && action.equalsIgnoreCase("Next") ){
				startEntry = lastScanResultId;
				resultsBefore = false;
			}
		}
		catch(NumberFormatException e){
			Dialog.getDialog(response, context, data, "The result identifier provided is invalid", "Invalid Parameter", DialogType.WARNING);
		}
		
		
		try{
			maxMinCount = HttpDefinitionScanResult.getScanResultInfo(scanResult.getScanResultID(), filter, Application.getApplication());
			findings = scanResult.getFindings(startEntry, RESULTS_PER_PAGE, filter, resultsBefore);
			
			if( findings.length > 0 ){
				firstScanResultId = findings[0].getScanResultID();
				lastScanResultId = findings[findings.length - 1].getScanResultID();
			}
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println(e.getMessage());
			findings = new HttpDefinitionScanResult[0]; //TODO deal with this exception
		}
		
		Vector<NameIntPair> definitionMatches;
		
		try{
			 definitionMatches = HttpDefinitionScanResult.getSignatureMatches(scanResult.getScanResultID());
		} catch( NoDatabaseConnectionException e ){
			throw new ViewFailedException(e);
		} catch( SQLException e ){
			throw new ViewFailedException(e);
		}
		
		Vector<NameIntPair> contentTypesCount = scanResult.getDiscoveredContentTypes();
		data.put("contentTypesCount", contentTypesCount);
		data.put("definitionMatches", definitionMatches);
		data.put("firstScanResultID", firstScanResultId);
		data.put("lastScanResultID", lastScanResultId);
		data.put("scanRuleFilter", scanRuleFilter);
		data.put("contentTypeFilter", contentTypeFilter);
		data.put("maxMinCount", maxMinCount);
		data.put("findings", findings);
		data.put("RESULTS_PER_PAGE", RESULTS_PER_PAGE);
		
		data.put("PENDING", ScanResultCode.PENDING);
		data.put("READY", ScanResultCode.READY);
		data.put("SCAN_COMPLETED", ScanResultCode.SCAN_COMPLETED);
		data.put("SCAN_FAILED", ScanResultCode.SCAN_FAILED);
		data.put("UNREADY", ScanResultCode.UNREADY);
		
		data.put("HIGH", Definition.Severity.HIGH);
		data.put("MEDIUM", Definition.Severity.MEDIUM);
		data.put("LOW", Definition.Severity.LOW);
		data.put("UNDEFINED", Definition.Severity.UNDEFINED);
		
		TemplateLoader.renderToResponse("WebDiscoveryResult.ftl", data, response);
		
		return true;
	}

}

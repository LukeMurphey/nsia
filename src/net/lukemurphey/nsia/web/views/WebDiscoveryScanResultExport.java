package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import net.lukemurphey.nsia.scan.DefinitionMatch;
import net.lukemurphey.nsia.scan.HttpDefinitionScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;

public class WebDiscoveryScanResultExport extends View {

	public static final String VIEW_NAME = "scan_result_export";

	public WebDiscoveryScanResultExport() {
		super("ScanResultExport", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the scan result and the associated findings
		HttpSeekingScanResult scanResult = (HttpSeekingScanResult)data.get("scanResult");
		
		HttpDefinitionScanResult[] findings = scanResult.getFindings();
		
		// 2 -- Create the file
		PrintWriter responseWriter = response.getWriter();
		
		CSVWriter writer = new CSVWriter(responseWriter);
		
		//	 2.1 -- Write out the header
		String[] header = new String[] { "URL", "Content Type", "Definition Name", "Severity", "Message" };
		writer.writeNext(header);
		
		//	 2.2 -- Write out the entries
		for (HttpDefinitionScanResult finding : findings) {
			
			DefinitionMatch[] matches = finding.getDefinitionMatches();
			
			String url = finding.getUrl().toExternalForm();
			String contentType = finding.getContentType();
			
			// If no matches exists, then print a row with no findings
			if( matches == null || matches.length == 0 ){
				writer.writeNext( new String[] { url, contentType, "", "", ""} );
			}
			
			// Otherwise, print a row for each definition matched
			else{
				for (DefinitionMatch definitionMatch : matches) {
					writer.writeNext( new String[] { url, contentType, definitionMatch.getDefinitionName(), definitionMatch.getSeverity().toString(), definitionMatch.getMessage()} );
				}
			}
		}
		
		return true;
	}

}

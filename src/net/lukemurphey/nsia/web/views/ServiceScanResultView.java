package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class ServiceScanResultView extends View {

	public static final String VIEW_NAME = "scan_result";
	
	public ServiceScanResultView() {
		super("ScanResult", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	public static String getURL( int scanResultID ) throws URLInvalidException{
		ServiceScanResultView view = new ServiceScanResultView();
		return view.createURL(scanResultID);
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		//ServiceScanResult scanResult = (ServiceScanResult)data.get("scanResult");
		data.put("TCP", NetworkPortRange.Protocol.TCP);
		data.put("UDP", NetworkPortRange.Protocol.UDP);
		data.put("CLOSED", NetworkPortRange.SocketState.CLOSED);
		data.put("OPEN", NetworkPortRange.SocketState.OPEN);
		data.put("NO_RESPONSE", NetworkPortRange.SocketState.NO_RESPONSE);
		data.put("UNDEFINED", NetworkPortRange.SocketState.UNDEFINED);
		
		TemplateLoader.renderToResponse("ServiceScanResult.ftl", data, response);
		
		return true;
	}

}

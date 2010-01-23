package net.lukemurphey.nsia.xmlRpcInterface;

import java.io.IOException;
import java.io.OutputStream;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Firewall;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.HostAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.xmlrpc.XmlRpcServer;

/**
 * This servlet facilitates XML-RPC calls.
 * @author luke
 *
 */
public class XmlrpcServlet extends HttpServlet {

	private static final long serialVersionUID = 420420903897281026L;

	private XmlRpcServer server;
	private Application application;
	
	//XML-RPC Handlers
	private XmlrpcSession sessionManagement;
	private XmlrpcFirewallManagement firewallManagement;
	private XmlrpcGroupManagement groupManagement;
	private XmlrpcUserManagement userManagement;
	private XmlrpcHttpHashScan httpHashScan;
	private XmlrpcScanData scanData;
	private XmlrpcSiteGroupManagement siteGroupManagement;
	private XmlrpcSystem system;
	private XmlrpcScannerController scannerController;
	
	
	public XmlrpcServlet(){
		application = Application.getApplication();
		sessionManagement = new XmlrpcSession(application);
		firewallManagement = new XmlrpcFirewallManagement(application);
		groupManagement = new XmlrpcGroupManagement(application);
		userManagement = new XmlrpcUserManagement(application);
		httpHashScan = new XmlrpcHttpHashScan();
		scanData = new XmlrpcScanData(application);
		siteGroupManagement = new XmlrpcSiteGroupManagement(application);
		system = new XmlrpcSystem(application);
		scannerController = new XmlrpcScannerController(application);
		
		server = new XmlRpcServer();
		registerHandlers( server );
	}
	
	/**
	 * Register the handlers for the remote procedure calls.
	 * @param server
	 */
	private void registerHandlers( XmlRpcServer server ){
		server.addHandler("SessionManagement",sessionManagement);
		server.addHandler("Firewall",firewallManagement);
		server.addHandler("GroupManagement",groupManagement);
		server.addHandler("UserManagement",userManagement);
		
		server.addHandler("HttpStatic",httpHashScan);
		server.addHandler("ScanData",scanData);
		server.addHandler("SiteGroupManagement",siteGroupManagement);
		server.addHandler("System",system);
		server.addHandler("ScannerController",scannerController);
	}
	
	/**
	 * XML-RPC does not support the GET method. This method should be invoked by a lost web browser; therefore, this
	 * method will simply redirect the user. 
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // 1.1 -- Set the server header
		response.setHeader("Server", "SiteSentry Web 0.1 (Alpha)");
        
        response.setContentType("text/xml");
        
   	 	// 1.2 -- Determine if the source is allowed
		Firewall firewall = Application.getApplication().getFirewall();
		Firewall.Action addressAllowed = firewall.isAllowed( new HostAddress( request.getRemoteAddr() ) );
		if( addressAllowed != Firewall.Action.ACCEPT && addressAllowed != Firewall.Action.ACCEPTED_BY_DEFAULT )
			response.sendError( HttpServletResponse.SC_FORBIDDEN );
		else
			response.sendRedirect("Console");
    }
	
	/**
	 * Service the XML-RPC call.
	 * @throws GeneralizedException 
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

       
		
        // 1.1 -- Set the server header
		response.setHeader("Server", "SiteSentry Web 0.1 (Alpha)");
        
        response.setContentType("text/xml");
        
   	 	// 1.2 -- Determine if the source is allowed
		/*Firewall firewall = Application.getApplication().getFirewall();
		int addressAllowed = firewall.isAllowed( InetAddress.getByName( request.getRemoteAddr() ) );
		if( addressAllowed != Firewall.FIREWALL_ACCEPT && addressAllowed != Firewall.FIREWALL_ACCEPTED_BY_DEFAULT )
			response.sendError( HttpServletResponse.SC_FORBIDDEN );*/
        
		// 1.3 -- Redirect if a lost browser
		//String redirect = "<script language=\"JavaScript\"><!--  window.location=\"Console\"; --></script>";
		
		// 1.4 -- Execure the XML-RPC command
        try{
			byte[] result = server.execute(request.getInputStream());
	        response.setContentLength(result.length);
	        
	        OutputStream out = response.getOutputStream();
	        out.write(result);
	        out.flush();
        }catch(IOException e){
        	e.printStackTrace();
        	//throw new GeneralizedException();
        }
        catch(NullPointerException e){
        	e.printStackTrace();
        	//throw new GeneralizedException();
        }
		catch(IllegalArgumentException e){
			e.printStackTrace();
        	//throw new GeneralizedException();
		}
        
    }

}

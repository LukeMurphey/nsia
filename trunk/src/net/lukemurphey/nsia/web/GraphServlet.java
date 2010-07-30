package net.lukemurphey.nsia.web;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NameIntPair;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.ApplicationStateMonitor.ApplicationStateDataPoint;
import net.lukemurphey.nsia.scan.*;
import net.lukemurphey.nsia.scan.Definition.Severity;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;
import net.lukemurphey.nsia.trustBoundary.ApiSessionManagement;

import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.servlet.*;
import org.jfree.ui.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.category.*;//For area chart
import org.jfree.data.general.*; //For pie chart data sets

public class GraphServlet extends HttpServlet {
	
	//private static final Object mutex = new Object();
	private static String lastStatusChartLocation = null;
	private static long lastStatusChartGenerationTime = 0;
	
	private static String lastRuleStatChartLocation = null;
	private static long lastRuleStatChartGenerationTime = 0;
	
	private static final long serialVersionUID = -6008582495779278653L;
	
	private static Hashtable<String,String> cachedCharts = new Hashtable<String,String>();
	
	public synchronized void doRequest(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

		//if(true){return;}
		
		// 1 -- Set the HTTP response headers
		int chartCreationFrequency = 10;
		response.setHeader("Server", WebConsoleServlet.SERVER_STRING);
		
		// 2 -- Make sure the user has a session
		boolean showGraph = true;
		SessionStatus sessionState;
		
		try {
			sessionState = getSessionState( request, response );
			
			if( sessionState != SessionStatus.SESSION_ACTIVE )
				showGraph = false;
		} catch (GeneralizedException e) {
			showGraph = false;
			return;
		}
		
		if( showGraph == false ){
			//response.setContentType("image/png");
			response.setHeader("Cache-Control", "max-age=" + 1 + ", must-revalidate");
			//response.setHeader("Expires", "Mon, 29 Jun 1998 02:28:12 GMT");
			response.getOutputStream().print("You must login to view this resource");
			return;
		}
		
		// 3 -- Get the graph size
		int width = -1;
		int height = -1;
		
		if( request.getParameter("W") != null ){
			try{
				width = Integer.parseInt( request.getParameter("W") );
			}
			catch(NumberFormatException e){
				//Retain the default value
			}
		}
		if( request.getParameter("H") != null ){
			try{
				height = Integer.parseInt( request.getParameter("H") );
			}
			catch(NumberFormatException e){
				//Retain the default value
			}
		}
		
		// 4 -- Generate the correct graph
		response.setContentType("image/png");
		response.setHeader("Cache-Control", "max-age=" + chartCreationFrequency + ", must-revalidate");
		ApplicationStateDataPoint[] metrics = Application.getApplication().getMetricsData();
		
		// 4.1 -- Graph displaying rules evaluated recently
		if( request.getPathInfo().matches("/RulesEvalGraph.png") || request.getPathInfo().matches("/RulesEvalGraph") ){
			if( lastRuleStatChartLocation == null || ( System.currentTimeMillis() - lastRuleStatChartGenerationTime ) > (chartCreationFrequency * 1000 ) ){
				JFreeChart chart = generateRuleEvaluationGraph( metrics );
				
				// 4.1.1 -- Set the chart configuration
				chart.setAntiAlias( true );
				chart.setBackgroundPaint( new Color(208,215,222) );// Text = 51, 81, 112
				chart.getLegend().setPosition( RectangleEdge.RIGHT );
				
				// 4.1.2 -- Output the chart
				if( width <= 0 ){
					width = 600;
				}
				if( height <= 0 ){
					height = 120;
				}
				
				lastRuleStatChartLocation = ServletUtilities.saveChartAsPNG( chart, width, height, null );
				
				lastRuleStatChartGenerationTime = System.currentTimeMillis();
				ServletUtilities.sendTempFile( lastRuleStatChartLocation, response );
			}
			else
				ServletUtilities.sendTempFile( lastRuleStatChartLocation, response );
		}
		
		// 4.2 -- Graph displaying content-types found during a scan
		if( request.getPathInfo().matches("/ContentTypeResults.png") || request.getPathInfo().matches("/ContentTypeResults") ){
			int scanResultID = -1;
			
			String resultIDString = request.getParameter("ResultID");
			
			if( resultIDString == null ){
				scanResultID = -1;
			}
			
			try{
				scanResultID = Integer.parseInt( resultIDString );
			}
			catch(NumberFormatException e){
				scanResultID = -1;
			}
			
			if( width <= 0 ){
				width = 400;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			// Returned the cached chart if one is available
			String hash = "ContentTypeResults/ResultID=" + scanResultID + "?W=" + width + "&H=" + height;
			if( cachedCharts.get(hash) != null ){
				ServletUtilities.sendTempFile( cachedCharts.get(hash), response );
				return;
			}
			
			JFreeChart chart = generateContentTypePieChart(getSessionIdentifier(request), scanResultID);
			
			// 4.2.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );// Text = 51, 81, 112
			
			// 4.2.2 -- Output the chart
			String ruleHistory = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			
			cachedCharts.put(hash, ruleHistory);
			
			ServletUtilities.sendTempFile( ruleHistory, response );
		}
		
		// 4.3 -- Graph displaying the severity of rules triggered during a scan
		else if( request.getPathInfo().matches("/SeverityResults.png") || request.getPathInfo().matches("/SeverityResults") ){
			int scanResultID = -1;
			
			String resultIDString = request.getParameter("ResultID");
			
			if( resultIDString == null ){
				scanResultID = -1;
			}
			
			try{
				scanResultID = Integer.parseInt( resultIDString );
			}
			catch(NumberFormatException e){
				scanResultID = -1;
			}
			
			if( width <= 0 ){
				width = 400;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			String hash = "SeverityResults/ResultID=" + scanResultID + "?W=" + width + "&H=" + height;
			if( cachedCharts.get(hash) != null ){
				ServletUtilities.sendTempFile( cachedCharts.get(hash), response );
				return;
			}
			
			JFreeChart chart = generateSeverityPieChart(getSessionIdentifier(request), scanResultID);
			
			// 4.3.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );
			
			// 4.3.2 -- Output the chart
			String severity = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			cachedCharts.put(hash, severity);
			ServletUtilities.sendTempFile( severity, response );
		}
		// 4.4 -- Graph displaying rules evaluated recently
		else if( request.getPathInfo().matches("/RuleScanHistory.png") || request.getPathInfo().matches("/RuleScanHistory") ){
			
			int scanRuleID = -1;
			
			String ruleIDString = request.getParameter("RuleID");
			
			if( ruleIDString == null ){
				scanRuleID = -1;
			}
			
			try{
				scanRuleID = Integer.parseInt( ruleIDString );
			}
			catch(NumberFormatException e){
				scanRuleID = -1;
			}
			
			// Identify the entries that should be displayed
			long startEntry = -1;
			String action =request.getParameter("Action");
			
			try{
				startEntry = Long.valueOf( request.getParameter("S") );
			}
			catch(NumberFormatException e){
				startEntry = -1;
			}
			
			if( action != null && action.equalsIgnoreCase("Previous") ){
				startEntry = Long.valueOf( request.getParameter("S") );
			}
			else if( action != null && action.equalsIgnoreCase("Next") ){
				startEntry = Long.valueOf( request.getParameter("E") );
			}
			
			JFreeChart chart = generateRuleHistoryGraph(getSessionIdentifier(request), scanRuleID, 20, startEntry);
			
			// 4.4.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );// Text = 51, 81, 112
			chart.getLegend().setPosition( RectangleEdge.RIGHT );
			
			// 4.4.2 -- Output the chart
			if( width <= 0 ){
				width = 650;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			String ruleHistory = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			ServletUtilities.sendTempFile( ruleHistory, response );
		}
		
		// 4.5 -- Graph displaying deviations relative to service scan
		else if( request.getPathInfo().matches("/ServiceScanDeviations.png") || request.getPathInfo().matches("/ServiceScanDeviations") ){
			int scanResultID = -1;
			
			String ruleIDString = request.getParameter("ResultID");
			
			if( ruleIDString == null ){
				scanResultID = -1;
			}
			
			try{
				scanResultID = Integer.parseInt( ruleIDString );
			}
			catch(NumberFormatException e){
				scanResultID = -1;
			}
			
			JFreeChart chart = generatePortScanDeviationsPieChart(getSessionIdentifier(request), scanResultID);
			
			// 4.5.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );
			
			// 4.5.2 -- Output the chart
			if( width <= 0 ){
				width = 350;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			String ruleHistory = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			ServletUtilities.sendTempFile( ruleHistory, response );
		}
		
		// 4.6 -- Graph displaying a summary of a UDP scan
		else if( request.getPathInfo().matches("/UDPSummary.png") || request.getPathInfo().matches("/UDPSummary") ){
			int scanResultID = -1;
			
			String ruleIDString = request.getParameter("ResultID");
			
			if( ruleIDString == null ){
				scanResultID = -1;
			}
			
			try{
				scanResultID = Integer.parseInt( ruleIDString );
			}
			catch(NumberFormatException e){
				scanResultID = -1;
			}
			
			JFreeChart chart = generatePortScanSummaryPieChart(getSessionIdentifier(request), scanResultID, NetworkPortRange.Protocol.UDP);
			
			// 4.6.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );
			
			// 4.6.2 -- Output the chart
			if( width <= 0 ){
				width = 350;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			String ruleHistory = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			ServletUtilities.sendTempFile( ruleHistory, response );
		}
		
		// 4.7 -- Graph displaying a summary of a TCP scan
		else if( request.getPathInfo().matches("/TCPSummary.png") || request.getPathInfo().matches("/TCPSummary") ){
			int scanResultID = -1;
			
			String ruleIDString = request.getParameter("ResultID");
			
			if( ruleIDString == null ){
				scanResultID = -1;
			}
			
			try{
				scanResultID = Integer.parseInt( ruleIDString );
			}
			catch(NumberFormatException e){
				scanResultID = -1;
			}
			
			JFreeChart chart = generatePortScanSummaryPieChart(getSessionIdentifier(request), scanResultID, NetworkPortRange.Protocol.TCP);
			
			// 4.7.1 -- Set the chart configuration
			chart.setAntiAlias( true );
			chart.setBackgroundPaint( new Color(208,215,222) );
			
			// 4.7.2 -- Output the chart
			if( width <= 0 ){
				width = 350;
			}
			if( height <= 0 ){
				height = 150;
			}
			
			String ruleHistory = ServletUtilities.saveChartAsPNG( chart, width, height, null );
			ServletUtilities.sendTempFile( ruleHistory, response );
		}
		
		// 4.8 -- Display a chart displaying the memory usage, thread executing, etc.
		else{
			if( lastStatusChartLocation == null || ( System.currentTimeMillis() - lastStatusChartGenerationTime ) > (chartCreationFrequency * 1000 ) ){
				JFreeChart chart = generateStatusGraph( metrics );
				
				// 4.5.1 -- Set the chart configuration
				chart.setAntiAlias( true );
				chart.setBackgroundPaint( new Color(208,215,222) );// Text = 51, 81, 112
				chart.getLegend().setPosition( RectangleEdge.RIGHT );
				
				// 4.6.2 -- Output the chart
				if( width <= 0 ){
					width = 600;
				}
				if( height <= 0 ){
					height = 120;
				}
				
				lastStatusChartLocation = ServletUtilities.saveChartAsPNG( chart, width, height, null );
				lastStatusChartGenerationTime = System.currentTimeMillis();
				ServletUtilities.sendTempFile( lastStatusChartLocation, response );
			}
			else
				ServletUtilities.sendTempFile( lastStatusChartLocation, response );
		}
		
		
		
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		try{
			doRequest( request, response );
		}
		catch(Exception e){
			//Application.getApplication().logExceptionEvent( net.lukemurphey.nsia.StringTable.MSGID_WEB_ERROR, e);
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response  ) throws ServletException, IOException {
		try{
			doRequest( request, response );
		}
		catch(Exception e){
			//Application.getApplication().logExceptionEvent( net.lukemurphey.nsia.StringTable.MSGID_WEB_ERROR, e);
		}
	}

	private static JFreeChart generateRuleHistoryGraph(String sessionIdentifier, int ruleID, int historyCount, long firstEntryID ){
		
		// 1 -- Create the series
		DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

		
		// 2 -- Get the data
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		ScanResult[] results = null;
		
		if( ruleID >= 0 ){
			try{
				if( firstEntryID > 0 ) {
					results = scanData.getScanResults(sessionIdentifier, firstEntryID, ruleID, historyCount, false);
				}
				else{
					results = scanData.getScanResults(sessionIdentifier, ruleID, historyCount);
				}
			}
			catch(Exception e){
				//Ignore this error, just show an empty chart
				results = new ScanResult[0];
			}
		}
		
		if(results == null){
			results = new ScanResult[0];
		}
		
		// 3 -- Add the data
		for(int c = results.length - 1; c >= 0; c--){

			dataSet.addValue( results[c].getIncompletes() , "Incomplete", "" + c);
			dataSet.addValue( results[c].getDeviations() , "Rejected", "" + c);
			dataSet.addValue( results[c].getAccepts() , "Passed", "" + c);
		}

		
		//Make sure at least one entry exists
		if( results.length == 0 ) {
			dataSet.addValue( 0 , "Incomplete", "");
			dataSet.addValue( 0 , "Rejected", "");
			dataSet.addValue( 0 , "Passed", "");
		}
		
		
		// 5 -- Generate the graph
		
		//	 5.1 -- Get the initial chart object
		JFreeChart chart = ChartFactory.createStackedBarChart(
					"", //Title
					"", // x-axis Label
					"", // y-axis Label
					dataSet,
					PlotOrientation.VERTICAL,
					true, //Legend
					false, //Tooltips
					false //URLs
		);
		

		CategoryPlot plot = (CategoryPlot)chart.getPlot();
		
		// 5.3 -- Set the plot options for the memory series
		GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
		renderer.setItemMargin(0.01);
		renderer.setDrawBarOutline(false);
		//renderer.setMinimumBarLength(400);
		
		renderer.setSeriesPaint(0, Color.YELLOW);
		renderer.setSeriesPaint(1, Color.RED);
		renderer.setSeriesPaint(2, Color.GREEN);	
		
		//	5.2 -- Map the 
		/*KeyToGroupMap keytogroupmap = new KeyToGroupMap("Incomplete");
		keytogroupmap.mapKeyToGroup("Incomplete", "Incomplete");
		keytogroupmap.mapKeyToGroup("Rejected", "Rejected");
		keytogroupmap.mapKeyToGroup("Passed", "Passed");
		renderer.setSeriesToGroupMap(keytogroupmap);*/
		
		
		plot.setRenderer(renderer);        
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the system state over time.
	 * @return
	 */
	private static JFreeChart generateStatusGraph(ApplicationStateDataPoint[] metrics){
		
		// 1 -- Create the series
		XYSeries series = new XYSeries("Memory Use (MB)");
		XYSeries threadSeries = new XYSeries("Threads");
		XYSeries dbConnectionSeries = new XYSeries("Database Connections");
		
		// 2 -- Get the metric data
		//ApplicationStateDataPoint[] metrics = Application.getApplication().getManager().getMetricsData();
		
		// 3 -- Add the data
		int d = 0;
		for( int c = 0; c < metrics.length; c++ ){
			if( metrics[c] != null ) {
				double usedMem = 1.0 * ( metrics[c].usedMemory / 1024.0 / 1024.0 );
				series.add( ((c+1) - metrics.length)/6.0, usedMem );
				
				threadSeries.add( ((c+1) - metrics.length)/6.0, metrics[c].threadCount );
				
				dbConnectionSeries.add( ((c+1) - metrics.length)/6.0, metrics[c].databaseConnections );
				d++;
			}
		}
		
		//Make sure at least one entry exists
		if( d == 0 )
			series.add( 0, 0 );
		
		// 4 -- Add the series to the data set
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series);
		dataset.addSeries(threadSeries);
		dataset.addSeries(dbConnectionSeries);
		
		// 5 -- Generate the graph
		
		//	 5.1 -- Get the initial chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				"",                // Title
				"Time (minutes)",   // x-axis Label
				"",                  // y-axis Label
				dataset,                   // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true,                      // Show Legend
				false,                      // Use tooltips
				false                      // Configure chart to generate URLs?
		);
		
		//	 5.2 -- Set the plot options for the memory series
		XYPlot plot = (XYPlot) chart.getPlot(); 	
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		
		renderer.setSeriesLinesVisible(0, true);
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesStroke(0, new BasicStroke(2));
		renderer.setSeriesPaint(0, new Color(255, 0, 0));//new Color(51, 81, 112)
		
		//	 5.3 -- Set the plot options for the thread count series
		renderer.setSeriesLinesVisible(1, true);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setSeriesStroke(1, new BasicStroke(2));
		renderer.setSeriesPaint(1, new Color(0, 192, 0));//new Color(51, 81, 112)
		//renderer.setSeriesPaint(1, new Color(51, 81, 112));
		//renderer.setSeriesPaint(1, new Color(0, 0,255));
		
		//	 5.4 -- Set the plot options for the database connection count series
		renderer.setSeriesLinesVisible(2, true);
		renderer.setSeriesShapesVisible(2, false);
		renderer.setSeriesStroke(2, new BasicStroke(2));
		renderer.setSeriesPaint(2, new Color(0, 0, 192));//new Color(51, 81, 112)
		//renderer.setSeriesPaint(1, new Color(51, 81, 112));
		//renderer.setSeriesPaint(1, new Color(0, 0,255));
		
		plot.setRenderer(renderer);        
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the rule evaluations over time.
	 * @return
	 */
	private static JFreeChart generateSeverityPieChart(String sessionIdentifier, int resultID){

		// 1 -- Create the series
		DefaultPieDataset dataset = new DefaultPieDataset();

		
		// 2 -- Get the data
		try{
			ApiScanData scanData = new ApiScanData(Application.getApplication());
			Hashtable<Severity, Integer> severities = scanData.getHTTPDefinitionMatchSeverities(sessionIdentifier, resultID);
			
			dataset.setValue("Informational", severities.get(Severity.UNDEFINED));
			dataset.setValue("Low", severities.get(Severity.LOW));
			dataset.setValue("Medium", severities.get(Severity.MEDIUM));
			dataset.setValue("High", severities.get(Severity.HIGH));
		}
		catch(NoSessionException e){
			//Ignore the exception, this chart will just display nothing
		}
		catch(GeneralizedException e){
			//Ignore the exception, this chart will just display nothing
		}
		
		/*
		HttpSeekingScanResult result = null;
		
		if( resultID >= 0 ){
			try{
				result = (HttpSeekingScanResult)scanData.getScanResult(sessionIdentifier, resultID);
			}
			catch(Exception e){
				//Ignore this error, just show an empty chart
			}
		}
		
		// 3 -- Add the data
		Hashtable<String, Integer> severities = new Hashtable<String, Integer>();
		severities.put("Info", 0);
		severities.put("Low", 0);
		severities.put("Medium", 0);
		severities.put("High", 0);
		
		if( result != null){
			HttpSignatureScanResult[] findings = result.getFindings();
			
			//	 3.1 -- Get the list of severities triggered
			for(int c = 0; c < findings.length; c++){
				
				SignatureMatch[] sigMatches  = findings[c].getSignatureMatches();
				if( sigMatches != null ){
					for(int d = 0; d < sigMatches.length; d++){

						if( sigMatches[d].getSeverity() == ContentSignature.Severity.LOW ){
							severities.put("Low", severities.get("Low") + 1);
						}
						else if( sigMatches[d].getSeverity() == ContentSignature.Severity.MEDIUM ){
							severities.put("Medium", severities.get("Medium") + 1);
						}
						else if( sigMatches[d].getSeverity() == ContentSignature.Severity.HIGH ){
							severities.put("High", severities.get("High") + 1);
						}
						else{
							severities.put("Info", severities.get("Info") + 1);
						}
					}
				}
			}

			//	 3.2 -- Add the list of severities to the chart
			dataset.setValue("Informational", severities.get("Info").intValue());
			dataset.setValue("Low", severities.get("Low").intValue());
			dataset.setValue("Medium", severities.get("Medium").intValue());
			dataset.setValue("High", severities.get("High").intValue());

		}*/
		
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart object
		JFreeChart chart = ChartFactory.createPieChart(
					"", //Title
					dataset,
					false, //Legend
					false, //Tooltips
					false //URLs
		);
		
		// 4.2 -- Set the plot options and colors
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setNoDataMessage("No data available");
		plot.setIgnoreNullValues(true);
		plot.setIgnoreZeroValues(true);
		plot.setSectionPaint(0, new Color(19, 82, 141));
		plot.setSectionPaint(1, new Color(254, 205, 49));
		plot.setSectionPaint(2, new Color(240, 154, 67));
		plot.setSectionPaint(3, new Color(206, 1, 1));
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the deviations noted after performing a port scan.
	 * @return
	 */
	private static JFreeChart generatePortScanDeviationsPieChart(String sessionIdentifier, int resultID){

		// 1 -- Create the series
		DefaultPieDataset dataset = new DefaultPieDataset();

		
		// 2 -- Get the data
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		final int UDP_OPEN = 0;
		final int UDP_CLOSED = 1;
		final int TCP_OPEN = 2;
		final int TCP_CLOSED = 3;
		
		NameIntPair[] deviations = new NameIntPair[4];
		
		deviations[UDP_OPEN] = new NameIntPair("UDP:open", 0);
		deviations[UDP_CLOSED] = new NameIntPair("UDP:closed", 0);
		deviations[TCP_OPEN] = new NameIntPair("TCP:open", 0);
		deviations[TCP_CLOSED] = new NameIntPair("TCP:closed", 0);
		
		NetworkPortRange[] diffs = null;
		
		try{
			ServiceScanResult result = (ServiceScanResult)scanData.getScanResult(sessionIdentifier, resultID);
			diffs = result.getDifferences();
		}
		catch(Exception e){
			//Ignore this exception; the trust boundary (reference the trust-boundary namespace) will have logged the error. 
		}
		
		// 3 -- Add the data		
		if( diffs != null){
			
			// 3.1 -- Count up the number of ports open and closed
			for(int c = 0; c < diffs.length; c++){
				
				if( diffs[c].getProtocol() == NetworkPortRange.Protocol.TCP && diffs[c].getState() == NetworkPortRange.SocketState.OPEN){
					deviations[TCP_OPEN].setValue(deviations[TCP_OPEN].getValue() + diffs[c].getNumberOfPorts());
				}
				else if( diffs[c].getProtocol() == NetworkPortRange.Protocol.TCP){
					deviations[TCP_CLOSED].setValue(deviations[TCP_CLOSED].getValue() + + diffs[c].getNumberOfPorts());
				}
				else if( diffs[c].getProtocol() == NetworkPortRange.Protocol.UDP && diffs[c].getState() == NetworkPortRange.SocketState.OPEN){
					deviations[UDP_OPEN].setValue(deviations[UDP_OPEN].getValue() + + diffs[c].getNumberOfPorts());
				}
				else{
					deviations[UDP_CLOSED].setValue(deviations[UDP_CLOSED].getValue() + + diffs[c].getNumberOfPorts());
				}
				
			}
			
			// 3.2 -- Add the data to chart
			for (NameIntPair devs : deviations) {
				if( devs.getValue() > 0){
					dataset.setValue(devs.getName(), devs.getValue());
				}
			}
			
		}
		
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart object
		JFreeChart chart = ChartFactory.createPieChart(
					"", //Title
					dataset,
					false, //Legend
					false, //Tooltips
					false //URLs
		);
		
		// 4.2 -- Set the plot options and colors
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setNoDataMessage("No deviations detected");
		
		plot.setSectionPaint(0, new Color(206, 1, 1));
		plot.setSectionPaint(1, new Color(255, 93, 0));
		plot.setSectionPaint(2, new Color(255, 131, 0));
		plot.setSectionPaint(3, new Color(255, 182, 0));
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the deviations noted after performing a port scan.
	 * @return
	 */
	private static JFreeChart generatePortScanSummaryPieChart(String sessionIdentifier, int resultID, NetworkPortRange.Protocol protocol){

		// 1 -- Create the series
		DefaultPieDataset dataset = new DefaultPieDataset();

		
		// 2 -- Get the data
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		final int OPEN = 0;
		final int CLOSED = 1;
		
		NameIntPair[] summary = new NameIntPair[2];
		
		summary[OPEN] = new NameIntPair("Open", 0);
		summary[CLOSED] = new NameIntPair("Closed", 0);
		//summary[NOT_RESPONDING] = new NameIntPair("Not Responding", 0);
		
		NetworkPortRange[] ports = null;
		
		try{
			ServiceScanResult result = (ServiceScanResult)scanData.getScanResult(sessionIdentifier, resultID);
			ports = result.getPortsScanned();
			//ports = result.getPortsExpectedOpen();
		}
		catch(Exception e){
			//Ignore this exception; the trust boundary (reference the trust-boundary namespace) will have logged the error. 
		}
		
		// 3 -- Add the data		
		if( ports != null){
			
			// 3.1 -- Count up the number of ports open and closed
			for(int c = 0; c < ports.length; c++){
				
				if( ports[c].getProtocol() == protocol && ports[c].getState() == NetworkPortRange.SocketState.OPEN){
					summary[OPEN].setValue(summary[OPEN].getValue() + ports[c].getNumberOfPorts());
				}
				/*else if( ports[c].getProtocol() == protocol && ports[c].getState()== NetworkPortRange.SocketState.CLOSED ){
					summary[CLOSED].setValue(summary[CLOSED].getValue() + ports[c].getNumberOfPorts());
				}
				else if( ports[c].getProtocol() == protocol && ports[c].getState() == NetworkPortRange.SocketState.NO_RESPONSE ){
					summary[NOT_RESPONDING].setValue(summary[NOT_RESPONDING].getValue() + ports[c].getNumberOfPorts());
				}*/
				else if (ports[c].getProtocol() == protocol) {
					summary[CLOSED].setValue(summary[CLOSED].getValue() + ports[c].getNumberOfPorts());
				}
				
			}
			
			// 3.2 -- Add the data to chart
			for (NameIntPair devs : summary) {
				dataset.setValue(devs.getName(), devs.getValue());
			}
			
		}
		
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart object
		JFreeChart chart = ChartFactory.createPieChart(
					"", //Title
					dataset,
					false, //Legend
					false, //Tooltips
					false //URLs
		);
		
		// 4.2 -- Set the plot options and colors
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setNoDataMessage("No data available");
		
		plot.setSectionPaint(0, new Color(173, 205, 221));
		plot.setSectionPaint(1, new Color(137, 184, 210));
		plot.setSectionPaint(2, new Color(82, 151, 184));
		//plot.setSectionPaint(3, new Color(32, 120, 157));
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the rule evaluations over time.
	 * @return
	 */
	private static JFreeChart generateContentTypePieChart(String sessionIdentifier, int resultID){

		// 1 -- Create the series
		DefaultPieDataset dataset = new DefaultPieDataset();

		
		// 2 -- Get the data
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		java.util.Vector<NameIntPair> mimeTypes = null;
		
		try{
			mimeTypes = scanData.getDiscoveredContentTypes(sessionIdentifier, resultID);
		}
		catch(Exception e){
			//Ignore this exception; the trust boundary (reference the trust-boundary namespace) will have logged the error. 
		}
		
		// 3 -- Add the data		
		if( mimeTypes != null){
			
			Iterator<NameIntPair> iterator = mimeTypes.iterator();
			
			while(iterator.hasNext()){
				NameIntPair currentItem = iterator.next();
				String currentKey = currentItem.getName();
				
				if(currentKey == null){
					currentKey= "[unknown]";
				}
				
				Integer count = currentItem.getValue();
				
				if( count != null ){
					// The content-type will be shortened in order to make it easier to read.
					String simplifiedKey = currentKey;
					
					if( currentKey.lastIndexOf("/x-") > 0 ){
						simplifiedKey = currentKey.substring(currentKey.lastIndexOf("/x-") + 3);
					}
					else if( currentKey.lastIndexOf("/") > 0 ){
						simplifiedKey = currentKey.substring(currentKey.lastIndexOf("/") + 1);
					}
					
					if(simplifiedKey.length() == 0){
						simplifiedKey = currentKey;
					}
					dataset.setValue(simplifiedKey, count);
				}
			}
		}
		
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart object
		JFreeChart chart = ChartFactory.createPieChart(
					"", //Title
					dataset,
					false, //Legend
					false, //Tooltips
					false //URLs
		);
		
		// 4.2 -- Set the plot options and colors
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setNoDataMessage("No data available");
		
		if( mimeTypes != null ){
			for(int c = 0; c < mimeTypes.size(); c++){
				int rem = c % 10;
				
				if( rem == 0 ){
					plot.setSectionPaint(c, new Color(173, 205, 221));
				}
				else if( rem == 1 ){
					plot.setSectionPaint(c, new Color(137, 184, 210));
				}
				else if( rem == 2 ){
					plot.setSectionPaint(c, new Color(82, 151, 184));
				}
				else if( rem == 3 ){
					plot.setSectionPaint(c, new Color(32, 120, 157));
				}
				else if( rem == 4 ){
					plot.setSectionPaint(c, new Color(181, 202, 189));
				}
				else if( rem == 5 ){
					plot.setSectionPaint(c, new Color(149, 181, 160));
				}
				else if( rem == 6 ){
					plot.setSectionPaint(c, new Color(99, 148, 110));
				}
				else if( rem == 7 ){
					plot.setSectionPaint(c, new Color(50, 117, 58));
				}
				else if( rem == 8 ){
					plot.setSectionPaint(c, new Color(198, 212, 176));
				}
				else if( rem == 9 ){
					plot.setSectionPaint(c, new Color(173, 196, 143));
				}
				else if( rem == 10 ){
					plot.setSectionPaint(c, new Color(134, 169, 83));
				}
				else {
					plot.setSectionPaint(c, new Color(96, 145, 46));
				}
			}
		}
		
		return chart;
	}
	
	/**
	 * Generate the chart depicting the rule evaluations over time.
	 * @return
	 */
	private static JFreeChart generateRuleEvaluationGraph(ApplicationStateDataPoint[] metrics){
		
		// 1 -- Create the series
		XYSeries rulesRejectedseries = new XYSeries("Rejected Rules");
		XYSeries rulesAcceptedSeries = new XYSeries("Accepted Rules");
		XYSeries rulesIncompleteSeries = new XYSeries("Incomplete Rules");
		
		// 2 -- Get the metric data
		//ApplicationStateDataPoint[] metrics = Application.getApplication().getManager().getMetricsData();
		
		// 3 -- Add the data
		int d = 0;
		for( int c = 0; c < metrics.length; c++ ){
			if( metrics[c] != null ) {
				rulesRejectedseries.add( ((c+1) - metrics.length)/6.0, metrics[c].rulesRejectedCount );
				
				rulesAcceptedSeries.add( ((c+1) - metrics.length)/6.0, metrics[c].rulesAcceptedCount );
				
				rulesIncompleteSeries.add( ((c+1) - metrics.length)/6.0, metrics[c].rulesIncompleteCount );
				d++;
			}
		}
		
		//Make sure at least one entry exists
		if( d == 0 ){
			rulesRejectedseries.add( 0, 0 );
			rulesAcceptedSeries.add( 0, 0 );
			rulesIncompleteSeries.add( 0, 0 );
		}
		
		// 4 -- Add the series to the data set
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(rulesRejectedseries);
		dataset.addSeries(rulesAcceptedSeries);
		dataset.addSeries(rulesIncompleteSeries);
		
		// 5 -- Generate the graph
		
		//	 5.1 -- Get the initial chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				"",                // Title
				"Time (minutes)",   // x-axis Label
				"",                  // y-axis Label
				dataset,                   // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true,                      // Show Legend
				false,                      // Use tooltips
				false                      // Configure chart to generate URLs?
		);
		
		//	 5.2 -- Set the plot options for the rules rejected
		XYPlot plot = (XYPlot) chart.getPlot(); 	
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		
		renderer.setSeriesLinesVisible(0, true);
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesStroke(0, new BasicStroke(2));
		renderer.setSeriesPaint(0, new Color(255, 0, 0));
		
		//	 5.3 -- Set the plot options for the rules accepted series
		renderer.setSeriesLinesVisible(1, true);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setSeriesStroke(1, new BasicStroke(2));
		renderer.setSeriesPaint(1, new Color(0, 192, 0));
		
		//	 5.4 -- Set the plot options for the rules incompletely scanned series
		renderer.setSeriesLinesVisible(2, true);
		renderer.setSeriesShapesVisible(2, false);
		renderer.setSeriesStroke(2, new BasicStroke(2));
		renderer.setSeriesPaint(2, new Color(255, 192, 0));
		
		plot.setRenderer(renderer);        
		
		return chart;
	}
	
	
	/*private static JFreeChart generateRuleEvaluationGraph( ApplicationStateDataPoint[] metrics ){
		
		// 1 -- Create the series
		double [][] data;
		int count = 1;
		if( metrics.length > 0 )
			count = metrics.length ;
		double [] scanRuleAccepted = new double[count];
		double [] scanRuleRejected = new double[count];
		double [] scanRuleIncomplete = new double[count];
		
		Double[] dataEntries = new Double[count];
		String[] dataTitles = new String[3]; // First arg = rows
		dataTitles[0] = "Rules Accepted";
		dataTitles[1] = "Rules Rejected";
		dataTitles[2] = "Rules Incomplete";
		
		// 2 -- Add the data
		int d = 0;
		for( int c = 0; c < metrics.length; c++ ){
			dataEntries[c] = new Double( ((c+1) - metrics.length)/6.0 );
			if( metrics[c] != null ) {
				scanRuleAccepted[c] = metrics[c].rulesAcceptedCount;
				scanRuleRejected[c] = metrics[c].rulesRejectedCount;
				scanRuleIncomplete[c] = metrics[c].rulesIncompleteCount;
				d++;
			}
		}
		
		//Make sure at least one entry exists
		if( d == 0 ){
			scanRuleAccepted[0] = 0;
			scanRuleRejected[0] = 0;
		}
		
		// 3 -- Add the series to the data set
		data = new double[3][count];
		data[0] = scanRuleAccepted;
		data[1] = scanRuleRejected;
		data[2] = scanRuleIncomplete;
		
		CategoryDataset dataset = DatasetUtilities.createCategoryDataset( dataTitles, dataEntries, data );
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart
		JFreeChart chart = ChartFactory.createStackedBarChart(
				"",                // Title
				"Rules Evaluated",   // x-axis Label
				"",                  // y-axis Label
				dataset,                   // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true,                      // Show Legend
				false,                      // Use tooltips
				false                      // Configure chart to generate URLs?
		);
		
		//	 4.2 -- Set the plot options for the memory series
		CategoryPlot plot = chart.getCategoryPlot();
		
		CategoryItemRenderer renderer = plot.getRenderer();
		//Paint greenGradient = new GradientPaint( 0.0f, 0.0f, new Color(0, 255, 0), 0.0f, 0.0f, new Color(196, 255, 196) );
		//renderer.setSeriesPaint(0, greenGradient);
		renderer.setSeriesPaint(0, new Color(0,255,0));
		
		//Paint redGradient = new GradientPaint( 0.0f, 0.0f, new Color(255, 0, 0), 0.0f, 0.0f, new Color(255, 150, 150) );
		//renderer.setSeriesPaint(1, redGradient );
		renderer.setSeriesPaint(1, new Color(255,0,0) );
		
		//Paint yellowGradient = new GradientPaint( 0.0f, 0.0f, new Color(255, 255, 0), 0.0f, 0.0f, new Color(255, 255, 196) );
		//renderer.setSeriesPaint(2, yellowGradient );
		renderer.setSeriesPaint(2, new Color(255,255,0) );
		
		plot.setRenderer(renderer);     
		
		return chart;
	}*/
	
	/*private static JFreeChart generateRuleEvaluationGraph( ApplicationStateDataPoint[] metrics ){
		
		// 1 -- Create the series
		double [][] data;
		int count = 1;
		if( metrics.length > 0 )
			count = metrics.length ;
		double [] scanRuleAccepted = new double[count];
		double [] scanRuleRejected = new double[count];
		double [] scanRuleIncomplete = new double[count];
		
		Integer[] dataEntries = new Integer[count];
		String[] dataTitles = new String[3]; // First arg = rows
		dataTitles[0] = "Rules Accepted";
		dataTitles[1] = "Rules Rejected";
		dataTitles[2] = "Rules Incomplete";
		
		// 2 -- Add the data
		int d = 0;
		for( int c = 0; c < metrics.length; c++ ){
			dataEntries[c] = new Integer( (-count+c) * 10 );
			if( metrics[c] != null ) {
				scanRuleAccepted[c] = metrics[c].rulesAcceptedCount;
				scanRuleRejected[c] = metrics[c].rulesRejectedCount;
				scanRuleIncomplete[c] = metrics[c].rulesIncompleteCount;
				d++;
			}
		}
		
		//Make sure at least one entry exists
		if( d == 0 ){
			scanRuleAccepted[0] = 0;
			scanRuleRejected[0] = 0;
		}
		
		// 3 -- Add the series to the data set
		data = new double[3][count];
		data[0] = scanRuleAccepted;
		data[1] = scanRuleRejected;
		data[2] = scanRuleIncomplete;
		
		CategoryDataset dataset = DatasetUtilities.createCategoryDataset( dataTitles, dataEntries, data );
		
		// 4 -- Generate the graph
		
		//	 4.1 -- Get the initial chart
		JFreeChart chart = ChartFactory.createStackedAreaChart(
				"",                // Title
				"Rules Evaluated",   // x-axis Label
				"",                  // y-axis Label
				dataset,                   // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true,                      // Show Legend
				false,                      // Use tooltips
				false                      // Configure chart to generate URLs?
		);
		
		//	 4.2 -- Set the plot options for the memory series
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setForegroundAlpha(0.5f);
		
		CategoryItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, new Color(0, 255, 0));
		renderer.setSeriesPaint(1, new Color(255, 0, 0));
		renderer.setSeriesPaint(2, new Color(255, 255, 0));
		//Re renderer = new CategoryItemRenderer;
		plot.setRenderer(renderer);     
		
		return chart;
	}*/
	
	private String getSessionIdentifier(HttpServletRequest request){
		
		Cookie[] cookies = request.getCookies();
		
		if( cookies == null ){
			return null;
		}
		
		for( int c = 0; c < cookies.length; c++ ){
			if( cookies[c].getName().matches("SessionID") )
				return cookies[c].getValue();
		}
		
		return null;
		
	}
	
	private SessionStatus getSessionState(HttpServletRequest request, HttpServletResponse response ) throws GeneralizedException{
		
		// 1 -- Determine the authentication status
		
		//	 1.1 -- Determine the session status
		Cookie[] cookies = request.getCookies();
		SessionStatus sessionStatus;
		String sessionIdentifier = null;
		
		if( cookies == null ){
			return SessionStatus.SESSION_NULL;
		}
		else{
			// Find the session ID cookie
			for( int c = 0; c < cookies.length; c++ ){
				if( cookies[c].getName().matches("SessionID") )
					sessionIdentifier = cookies[c].getValue();
			}
			
			ApiSessionManagement xSession = new ApiSessionManagement( Application.getApplication() );
			sessionStatus = SessionStatus.getStatusById( xSession.getSessionStatus( sessionIdentifier ) );
			
			if( sessionStatus.equals( SessionStatus.SESSION_IDENTIFIER_EXPIRED ) ){
				sessionIdentifier = xSession.refreshSessionIdentifier( sessionIdentifier );
				if( sessionIdentifier == null )
					sessionStatus = SessionStatus.SESSION_NULL;
				else{
					sessionStatus = SessionStatus.getStatusById( xSession.getSessionStatus( sessionIdentifier ) );
					response.addCookie( new Cookie( "SessionID", sessionIdentifier) );
					
				}
			}
		}
		
		return sessionStatus;
	}
	
}

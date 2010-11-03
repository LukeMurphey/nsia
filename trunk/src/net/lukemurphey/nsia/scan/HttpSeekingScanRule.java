package net.lukemurphey.nsia.scan;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.commons.httpclient.HttpClient;
import org.htmlparser.*;
import org.htmlparser.visitors.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;

import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.HttpDefinitionScanRule.HttpSignatureScanResultWithParser;

/**
 * This rule is designed to automatically search through a website for content that appears to be malicious. This rule will auto-discover content by following links
 * from other resources. The benefit of this rule is that it can be used to scan content even if it is not specifically identified. This eliminates the need for the
 * someone to specifically load all web resources for a website into the application. This is especially important since many times the hidden resources are often
 * the ones most likely to contain content that is notable or problematic.
 * @author luke
 * 
 */

public class HttpSeekingScanRule extends ScanRule implements WorkerThread {

	/**
	 * Describes the result of the analysis.
	 * @author luke
	 *
	 */
	public enum FindingResult{
		PARSE_FAILED, CONNECTION_FAILED, NOT_FOUND, PASSED, DEVIATIONS_DETECTED, URI_INVALID
	}
	
	/**
	 * This class represents a resource that was analyzed. The class will include a list of the signatures matched (if any match).
	 * @author luke
	 *
	 */
	public static class Finding{
		private URL url;
		private DefinitionMatch[] signatureMatches;
		private long scanRuleId;
		private FindingResult findingResult;
		
		public Finding( URL url, DefinitionMatch[] matches, long scanRuleId, FindingResult findingResult){
			
			this.url = url;
			signatureMatches = new DefinitionMatch[matches.length];
			
			System.arraycopy(matches, 0, signatureMatches, 0, matches.length);
			
			this.scanRuleId = scanRuleId;
			this.findingResult = findingResult;
		}
		
		public Finding( URL url, Vector<DefinitionMatch> matches, long scanRuleId, FindingResult findingResult){
			
			this.url = url;
			signatureMatches = new DefinitionMatch[matches.size()];
			matches.toArray(signatureMatches);
			this.scanRuleId = scanRuleId;
			this.findingResult = findingResult;
		}
		
		public Finding( URL url, long scanRuleId, FindingResult findingResult){
			this(url, new Vector<DefinitionMatch>(), scanRuleId, findingResult);
		}
		
		public URL getUrl(){
			return url;
		}
		
		public FindingResult result(){
			return findingResult;
		}
		
		public DefinitionMatch[] getSignatureMatches(){
			
			DefinitionMatch[] matches = new DefinitionMatch[signatureMatches.length];
			
			System.arraycopy(signatureMatches, 0, matches, 0, signatureMatches.length);
			
			return matches;
		}
		
		public long getScanRuleID(){
			return scanRuleId;
		}
	}
	
	/**
	 * This class is intended to identify linked resources that should be scanned.
	 * @author luke
	 *
	 */
	public static class LinkExtractionVisitor extends NodeVisitor
	{
		private URL parentURL;
		private Vector<URL> urls = new Vector<URL>();
		private int nodes = 0;
		
	    public LinkExtractionVisitor (URL parentURL)
	    {
	    	// 0 -- Precondition check
	    	if( parentURL == null ){
	    		throw new IllegalArgumentException("The parent URL cannot be null");
	    	}
	    	
	    	// 1 -- Set the field
	    	this.parentURL = parentURL;
	    }
	    
	    public LinkExtractionVisitor (URL parentURL, boolean shouldRecurseChildren)
	    {
	    	super(shouldRecurseChildren);
	    	
	    	// 0 -- Precondition check
	    	if( parentURL == null ){
	    		throw new IllegalArgumentException("The parent URL cannot be null");
	    	}
	    	
	    	// 1 -- Set the field
	    	this.parentURL = parentURL;
	    }

	    public void visitTag (Tag tag)
	    {
	    	
	    	try{

	    		nodes++;

		    	if( tag.getTagName().equalsIgnoreCase("a") && tag.getAttribute("href") != null){
		    		urls.add(new URL(this.parentURL, tag.getAttribute("href")));
		    	}
		    	else if( tag.getTagName().equalsIgnoreCase("img") && tag.getAttribute("src") != null){
		    		urls.add(new URL(this.parentURL, tag.getAttribute("src")));
		    	}
	    	}
	    	catch(MalformedURLException e){
	    		//Ignore
	    	}
	    }

	    public Vector<URL> getExtractedURLs(){
	    	return urls;
	    }
	    
	    public void visitStringNode (Text string)
	    {

	    }
	}
	
	/* Provides a list of resources that will be used to start the scanning process (will start by following links from the
	 * URLs provided below.
	 */
	private Vector<URL> seedUrls = new Vector<URL>();
	
	/* The wilcard will be evaluated to determine if the resource should be scanned. If the domain of a resource does not
	 * match the wildcard below, then it will be considered external to the resource to be scanned.
	 */
	private Wildcard restrictToDomain = null;
	
	// Indicates if the external resources directly linked to the resource should be scanned.
	private boolean scanExternalLinks = false;
	
	/* The following two fields are intended to prevent the scanner from entering an endless loop as well as preventing it
	 * from scanning too much (and thus never finishing). The rule will stop evaluating when either of the limiters below
	 * include the rule has gone far enough (that is, stops on the more restrictive limit).  
	 */
	private int scanCountLimit = 1000; //The maximum number of resources to scan 
	private int recursionLevels = 10;  //The maximum levels deep to go when following embedded links.
	
	/* The following static fields set the maximum limit that can possibly be set for the scanCountLimit and recursionLevels
	 * fields.
	 */
	private static final int DEPTH_LIMIT = 100;
	private static final int SCAN_LIMIT = 50000;
	
	//Indicates the number of resources the rule has scanned. This is updated while the rule is being evaluated and can thus be used to determine progress. 
	private int resourcesScanned = 0;
	
	//The following fields are used to derive the state of the rule (scanning, stopped, etc).
	private boolean terminate = false;
	private boolean inScan = false;
	
	//This field indicates the resource that is currently being scanned.
	private String currentlyScanning;
	
	//This field contains the last scan result if the run method is called.
	private HttpSeekingScanResult lastScanResult = null;
	
	//This field contains any exceptions throw during the operation
	private Exception exceptionThrown = null;
	
	//This field prevents the scanner from discovering URLs within resources that returned a 404 error. This prevents endless recursion caused by broken relative references.
	private boolean stopRecursingOn404 = true;
	
	//This contains the list of thread in process. These are retained so that the rule can be terminated
	Vector<ScanRunner> runningThreads = new Vector<ScanRunner>();
	
	//Sets a limit on the number of scan threads that will be created when performing multi-threaded scans
	int maxScanThreads = 10;
	
	public static final String RULE_TYPE = "HTTP/Autodiscovery";
	
	public static final int SUBCATEGORY_EXCEPTION_THRESHOLD = 5;
	
	public HttpSeekingScanRule(Application appRes ){
		super(appRes);
		
		try {
			setMaxScanThreads(appRes.getApplicationConfiguration().getMaxHTTPScanThreads());
		} catch (NoDatabaseConnectionException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (SQLException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (InputValidationException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		}
	}
	
	public HttpSeekingScanRule(Application appRes, Wildcard restrictTo, int scanFrequency, boolean includeFirstLevelOfExternalLinks ){
		super(appRes);
		
		setDomainRestriction(restrictTo);
		setScanFrequency(scanFrequency);
		
		try {
			setMaxScanThreads(appRes.getApplicationConfiguration().getMaxHTTPScanThreads());
		} catch (NoDatabaseConnectionException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (SQLException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (InputValidationException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		}
	}

	public HttpSeekingScanRule(Application appRes, Wildcard restrictTo, boolean includeFirstLevelOfExternalLinks ){
		super(appRes);
		
		setDomainRestriction(restrictTo);
		setScanFrequency(3600);
		
		try {
			setMaxScanThreads(appRes.getApplicationConfiguration().getMaxHTTPScanThreads());
		} catch (NoDatabaseConnectionException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (SQLException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		} catch (InputValidationException e) {
			appRes.getEventLog().logEvent(new EventLogMessage(EventType.INTERNAL_ERROR));
		}
	}
	
	public void scanExternalLinks( boolean includeFirstLevelOfExternalLinks ){
		scanExternalLinks = includeFirstLevelOfExternalLinks;
	}
	
	public boolean getScansExternalLinks( ){
		return scanExternalLinks;
	}
	
	/**
	 * Get the wildcard that determines which URLs will be scanned.
	 * @return
	 */
	public Wildcard getDomainRestriction(){
		return restrictToDomain;
	}
	
	public void setDomainRestriction(Wildcard restrictTo){
		
		// 0 -- Precondition check
		if( restrictTo == null ){
			throw new IllegalArgumentException("The domain restriction wildcard cannot be null");
		}
		
		// 1 -- Set the parameter
		this.restrictToDomain = restrictTo;
	}
	
	private Vector<URL> extractUrls(URL url, Parser parser) throws UnsupportedEncodingException{
		
		// 1 -- Extract all references from the document parse by the given parser
		Vector<URL> urls = new Vector<URL>();
		Vector<URL> urlsTemp;
		
		//	 1.1 -- hrefs <a href=?>
		urlsTemp = getUrlAttrs("a", "href", url, parser, false);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.2 -- images <img src=?>
		urlsTemp = getUrlAttrs("img", "src", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.3 -- applet <applet code=?>
		urlsTemp = getUrlAttrs("applet", "code", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.4 -- object <object codebase=?>
		urlsTemp = getUrlAttrs("object", "codebase", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.5 -- links <link href=?>
		urlsTemp = getUrlAttrs("link", "href", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.6 -- script <script src=?>
		urlsTemp = getUrlAttrs("script", "src", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.7 -- inline frames
		urlsTemp = getUrlAttrs("iframe", "src", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.8 -- frames
		urlsTemp = getUrlAttrs("frame", "src", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.9 -- embed
		urlsTemp = getUrlAttrs("embed", "code", url, parser, true);
		
		if( urlsTemp != null ){
			urls.addAll(urlsTemp);
		}
		
		//	 1.9 -- Shockwave Flash objects declared in JavaScript (this is the preferred way to load Flash). SWFObject[ ]*\([ ]*"([-0-9.a-z_A-Z/\\]*)
		//TODO Load Shockware flash references made using the SWFObject 
		
		// 2 -- Return the result
		return urls;
		
	}
	
	/**
	 * This exception handler manages exceptions thrown by scanners.
	 * @author Luke Murphey
	 *
	 */
	private class ScanThreadExceptionHandler implements Thread.UncaughtExceptionHandler {
		//private Application application;
		private URL url; 
		
		public ScanThreadExceptionHandler(URL url){
			this.url = url;
		}
		
		public void uncaughtException(Thread t, Throwable e){
			EventLogMessage message = new EventLogMessage(EventType.SCAN_ENGINE_EXCEPTION, new EventLogField(FieldName.RULE_ID, scanRuleId ), new EventLogField(FieldName.URL, url.toString() ));
			appRes.logExceptionEvent(message, e);
		}
	}
	
	/**
	 * Perform a scan using multiple threads. This method creates multiple ScanRunner instances to perform actual scans.
	 * @param sigs
	 * @param maxThreads
	 * @param findings
	 * @throws UnsupportedEncodingException 
	 */
	private void multiThreadedScan(DefinitionSet sigs, int maxThreads, Vector<HttpDefinitionScanResult> findings ) throws UnsupportedEncodingException{
		
		// 1 -- Initialize the scanner
		
		// This vector contains the records that are pending
		Vector<ScanRecord> pending = new Vector<ScanRecord>();
		
		//List of URLS that have been or are being processed
		Vector<String> urls = new Vector<String>();
		
		HttpClient client = new HttpClient(HttpDefinitionScanRule.getConnectionManager());
		
		// 1.1 -- Add all of preset URLs to the list
		for( URL url : this.seedUrls){
			urls.add( URLDecoder.decode(url.toString(), "UTF-8") );
			pending.add( new ScanRecord(url) );
		}
		
		
		// 2 -- Loop through each URL and scan them until the limit is reached
		while( ( (findings.size() + runningThreads.size()) < this.scanCountLimit && terminate == false ) ){
			
			//Terminate if no more URLs exist and all of the threads are done 
			if ( runningThreads.size() == 0 && pending.size() == 0 ){
				break;
			}
			
			// 2.1 -- Loop through the threads and process the results. This will remove completed threads from the list of running threads. 
			Iterator<ScanRunner> it = runningThreads.iterator();
			while ( it.hasNext() ){
				ScanRunner runner = it.next();

				if( runner.done() ){

					// 2.3.1 -- Move completed items to the finished queue 
					it.remove();
					HttpSignatureScanResultWithParser result = runner.getResult();

					if( terminate == false && result != null ){
						findings.add(result.getScanResult());
						resourcesScanned++;

						// 2.3.2 -- Extract the URLs to scan and add them to the list
						if( runner.getLevel() < recursionLevels && result.getParser() != null ){
							
							// Make sure the content-type is HTML
							if( result.getScanResult() != null ){
								
								Vector<URL> extractedUrls = null;
								
								// Extract the URLs from the HTML file (if it is an HTML)
								if( result.getScanResult().getContentType() == null
									|| result.getScanResult().getContentType().contains("html")
									|| result.getScanResult().getContentType().contains("xml") ){
									
									extractedUrls = extractUrls(result.getScanResult().getUrl(), result.getParser());
								}
								else{
									extractedUrls= new Vector<URL>();
								}

								// Add any URLs provided by the definitions
								extractedUrls.addAll( result.getExtractURLs() );
								
								for( URL newURL : extractedUrls ){

									// Check to see if we have hit the limit. If so, then stop. 
									if( findings.size() >= this.scanCountLimit ){
										break;
									}

									String str = newURL.toString();
									boolean found = false;

									// See if the entry is in the list or URLs already processed
									for( String oldURL : urls ){
										if( oldURL.equalsIgnoreCase(str) ){
											found = true;
										}
									}

									// The URL appears to be unique, add it to the list
									if( found == false ){

										urls.add(str);

										/*
										 * If the resource returned a 404 response code, then only scan the other resources directly included in the page with the 404. This prevent loops
										 * caused by 404 pages that include references to same the same 404 page. To prevent recursion, set a recursion level to the max so that the scanner
										 * won't recurse further.
										 */ 
										if( result.getHttpResponseCode() == 404 && stopRecursingOn404 ){
											pending.add(new ScanRecord(newURL, result.getScanResult(), ( recursionLevels ) ));
										}
										else{
											pending.add(new ScanRecord(newURL, result.getScanResult(), ( runner.getLevel() + 1 ) ));
										}
									}
								}
							}
						}
					}
				}
			}
			
			// 2.2 -- Check to make sure that the thread limit has been reached. If it has, then wait until a spot opens up. Also, pause if no URLs exist to be scanned (if the pending queue is empty).
			if( runningThreads.size() >= maxThreads || pending.size() == 0 ){
				try{
					Thread.sleep(1000);
				}
				catch(InterruptedException e){
					//Ignore, this just means that the thread the sleep call was interrupted
				}
				
				continue; //Restart the loop, hopefully a thread will have completed and thus will leave a spot open
			}
			
			// 2.3 -- Create a scan thread from one of the pending items and put it in the list of executing threads
			else if( terminate == false && pending.size() > 0 ){
				ScanRecord record = pending.remove(0); //Remove the URL from the pending list
				ScanRunner runner = new ScanRunner( record, sigs, record.getLevel(), client );
				runningThreads.add(runner);

				runner.setUncaughtExceptionHandler(new ScanThreadExceptionHandler(record.getURL()));
				runner.setPriority(	Thread.MIN_PRIORITY );
				runner.start();
			}
		}
		
		// 3 -- Wait for any executing threads to complete
		while(runningThreads.size() > 0){
			Iterator<ScanRunner> it = runningThreads.iterator();
			while ( it.hasNext() ){
				ScanRunner runner = it.next();
				
				if( runner.done() ){
					it.remove();
					HttpSignatureScanResultWithParser result = runner.getResult();

					if( terminate == false && result != null ){
						findings.add(result.getScanResult());
						result = null;
						resourcesScanned++;
					}
				}
				else{
					try{
						Thread.sleep(1000);
					}
					catch(InterruptedException e){
						//Ignore, this just means that the thread the sleep call was interrupted
					}
				}
			}
		}
	}
	
	/**
	 * The ScanRunner performs a scan and retrieves the result.
	 * @author Luke
	 *
	 */
	private class ScanRunner extends Thread{
		
		private HttpSignatureScanResultWithParser result;
		private HttpDefinitionScanRule rule;
		private HttpDefinitionScanResult parentScanResult = null;
		private boolean done= false;
		private int level = 0;
		
		public ScanRunner(ScanRecord record, DefinitionSet signatureSet, int level, HttpClient client){
			this.rule = new HttpDefinitionScanRule(appRes, signatureSet, record.url, client);
			this.level = level;
			this.rule.setCallback( callback );
			
			if( record.parentScanResult != null ){
				this.parentScanResult = record.parentScanResult;
			}
			
			this.setName("HTTP Seeking Scan Rule: " + record.url.toString());
		}
		
		public int getLevel(){
			return level;
		}
		
		public HttpSignatureScanResultWithParser getResult(){
			return result;
		}
		
		public void run(){
			try{
				rule.suppressLoggingToEventLog(true);//Suppress the log messages because scan results for linked pages may update the scan result if they find a broken link 
				rule.scanRuleId = scanRuleId; //This is necessary so that the rule can load the relevant exceptions
				result = rule.doScanAndReturnParser(parentScanResult);
			}
			catch(IllegalStateException e){
				//This exception occurs with protocols that are unsupported (such as mailto); ignore it.
				result = null;
			}
			catch(Exception e){
				if( this.getUncaughtExceptionHandler() != null ){
					this.getUncaughtExceptionHandler().uncaughtException(this, e);
				}
				exceptionThrown = e;
			}
			finally{
				done = true;
			}
		}
		
		public void terminate(){
			rule.terminate();
		}
		
		public boolean done(){
			return done;
		}
	}
	
	/**
	 * Records the result of a scan.
	 * @author Luke
	 *
	 */
	private static class ScanRecord{
		private URL url;
		private HttpDefinitionScanResult parentScanResult;
		private int currentLevel;
		
		public ScanRecord( URL url, HttpDefinitionScanResult parentScanResult, int currentLevel ){
			this.url = url;
			this.parentScanResult = parentScanResult;
			this.currentLevel = currentLevel;
		}
		
		public ScanRecord( URL url ){
			this.url = url;
			this.parentScanResult = null;
			this.currentLevel = 0;
		}
		
		public URL getURL(){
			return url;
		}
		
		public int getLevel(){
			return currentLevel;
		}
	}
	
	/**
	 * Extracts all URLs from tags with the defined attribute.
	 * @param tag
	 * @param attribute
	 * @param parentUrl
	 * @param htmlDocumentParser
	 * @param alwaysInclude
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private Vector<URL> getUrlAttrs(String tag, String attribute, URL parentUrl, Parser htmlDocumentParser, boolean alwaysInclude) throws UnsupportedEncodingException{
		
		try{
			//LinkExtractionVisitor visitor = new LinkExtractionVisitor(parentUrl, true);
			//htmlDocumentParser.visitAllNodesWith(visitor);
			//return visitor.getExtractedURLs();
			
			TagNameFilter tagNameFilter = new TagNameFilter(tag);
			NodeList nodesList = htmlDocumentParser.extractAllNodesThatMatch(tagNameFilter);
			return getUrlAttrs(nodesList, attribute, parentUrl, alwaysInclude);
			
		}
		catch( ParserException e){
			// Ignore, just move on to the next item
			return null;
		}
		finally{
			if( htmlDocumentParser != null){
				htmlDocumentParser.reset();
			}
		}
	}
	
	/**
	 * Determine if the domain matches the domain limiter. This method will consider whether the wildcard
	 * should be considered a domain name, path or complete URL with arguments.
	 * @param url
	 * @return
	 */
	private boolean domainMatches( URL url ){
		
		// 1 -- See if the domain matcher has arguments
		if( restrictToDomain.wildcard().contains("?") ){
			return restrictToDomain.getPattern().matcher( url.toString() ).matches();
		}
		
		// 2 -- See if it is just a domain 
		else if( Pattern.matches("[0-9a-zA-Z*-.]+", restrictToDomain.wildcard()) ){
			return restrictToDomain.getPattern().matcher( url.getHost() ).matches();
		}
		
		// 3 -- Otherwise, assume the entire URL ought to be matched
		else{
			String[] urlNoArgs = url.toString().split("[?]");
			return restrictToDomain.getPattern().matcher( urlNoArgs[0] ).matches();
		}
	}
	
	/**
	 * Gets a list of URLs built from paths included in the given attribute (like "href", "src", etc.).
	 * @param nodesList The list of nodes to search
	 * @param attribute The name of the attribute to find URLs in (like "href" or "src")
	 * @param parentUrl The URL of the parent, will be used to create absolute URLs from relative ones.
	 * @param alwaysInclude WIll include URL in the list even if the URL does not match the given domain.
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private Vector<URL> getUrlAttrs(NodeList nodesList, String attribute, URL parentUrl, boolean alwaysInclude) throws UnsupportedEncodingException{
		
		Vector<URL> referenceList = new Vector<URL>();
		
		for( int c = 0; c < nodesList.size(); c++ ){
			Tag node = (Tag)nodesList.elementAt(c);
			
			String currentItem = node.getAttribute(attribute);
			
			if( currentItem != null ){
				try{
					
					/* Remove the fragment (the content behind the "#" symbol) since this symbol is not sent to the
					 * server and should not change the resulting output. This symbol is only used by the browser to
					 * determine where on the page to scroll to. 
					 */  
					int fragmentOffset = currentItem.lastIndexOf("#");
					
					if( fragmentOffset >= 0 ){
						currentItem = currentItem.substring(0, fragmentOffset);
					}
					
					URL newURL = new URL(parentUrl, URLDecoder.decode(currentItem.trim(), "UTF-8") );
					
					if( newURL.getHost() != null && hostnameIsValid(newURL.getHost()) ){ //This check was added because the scanner kept trying to scan "http://:". Unable to determine the exact root cause but it seems to be related to  
						//Add the entry to the list of URLs
						if (	// Don't add the URL unless it is either...
								// 1) within the specified domain
								domainMatches( newURL )
								// 2) the link is the first level of external links the system was setup to scan external links 
								|| (scanExternalLinks && domainMatches( parentUrl ) )
								// 3) always include flag is true
								|| alwaysInclude == true
								)
						{
							referenceList.add( newURL );
						}
					}
				}
				catch(MalformedURLException e){
					//Ignore this entry, it is not a valid URL
				}
			}
		}
		
		return referenceList;
	}
	
	/**
	 * Returns a boolean indicating if the hostname is valid.
	 * @param hostname
	 * @return
	 */
	private boolean hostnameIsValid( String hostname ){
		return Pattern.matches("[-.0-9a-zA-Z]+", hostname);
	}
	
	@Override
	public void delete() throws SQLException, NoDatabaseConnectionException {
		Connection connection = null;
		PreparedStatement statement = null;
		
		super.deleteRule(scanRuleId);
		
		try{
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Delete from HttpDiscoveryRule where ScanRuleID = ?");
			statement.setLong(1, scanRuleId);
			
			statement.execute();
		}
		finally{
			
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
		}
	}

	public void setMaxScanThreads( int max ){
		
		if( max < 1 ){
			throw new IllegalArgumentException("The maximum number of threads cannot be less than one");
		}
		
		maxScanThreads = max;
	}
	
	public int getMaxScanThreads(){
		return maxScanThreads;
	}
	
	@Override
	public ScanResult doScan() throws ScanException {
		
		try{
			inScan = true;
			Vector<HttpDefinitionScanResult> findings = new Vector<HttpDefinitionScanResult>();
			DefinitionSet sigs;
			
			// 1 -- Load the signatures
			try{
				sigs = DefinitionArchive.getArchive().getDefinitionSet();
			}
			catch(NoDatabaseConnectionException e){
				throw new ScanException("Signature set could not be loaded", e);
			}
			catch(DefinitionSetLoadException e){
				throw new ScanException("Signature set could not be loaded", e);
			}
			catch(SQLException e){
				throw new ScanException("Signature set could not be loaded", e);
			}
			catch(InputValidationException e){
				throw new ScanException("Signature set could not be loaded", e);
			}
			
			// 2 -- Scan the seed URLs
			try {
				multiThreadedScan(sigs, maxScanThreads, findings);
			} catch (UnsupportedEncodingException e) {
				throw new ScanException("Cannot perform a scan since the encoding required to decode URLs is not-supported by the runtime", e);
			}
			
			// 3 -- Determine if any of the scan attempts failed
			ScanResultCode resultCode = null;
			
			for(int c = 0; c < findings.size(); c++){
				if( findings.get(c).getResultCode().getId() != ScanResultCode.SCAN_COMPLETED.getId() && resultCode == null){
					resultCode = findings.get(c).getResultCode();
				}
				else if(findings.get(c).getResultCode().getId() != ScanResultCode.SCAN_COMPLETED.getId() && resultCode != null && resultCode.getId() != findings.get(c).getResultCode().getId() ){
					resultCode = ScanResultCode.SCAN_FAILED;
					break;
				}
			}
			
			if( terminate == true ){
				resultCode = ScanResultCode.SCAN_TERMINATED;
			}
			// Return a scan failed code if no resources are scanned
			else if( findings.size() == 0 ){
				resultCode = ScanResultCode.SCAN_FAILED;
			}
			
			// 4 -- Log the results
			for(int c = 0; c < findings.size(); c++){
				logSignatureScanResult(findings.get(c).deviations, findings.get(c).getUrl());
			}
			
			// 5 -- Indicate that the scan is completed and exit
			inScan = false;
			
			if( resultCode != null){
				return new HttpSeekingScanResult( findings, restrictToDomain.wildcard(), scanRuleId, resultCode, new java.sql.Timestamp(System.currentTimeMillis()) );
			}
			else{
				return new HttpSeekingScanResult( findings, restrictToDomain.wildcard(), scanRuleId, ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()) );
			}
			
		}
		finally{
			inScan = false;
		}
	}
	
	private void logSignatureScanResult( int definitionsMatched, URL url ){
		if( definitionsMatched > 0 ){
			logScanResult( ScanResultCode.SCAN_COMPLETED, definitionsMatched, HttpSeekingScanRule.RULE_TYPE, url.toString(), definitionsMatched + " definitions matched" );
		}
		else{
			logScanResult( ScanResultCode.SCAN_COMPLETED, 0, HttpSeekingScanRule.RULE_TYPE, url.toString(), "0 definitions matched" );
		}
	}
	
	@Override
	public String getRuleType() {
		return RULE_TYPE;
	}

	@Override
	public String getSpecimenDescription() {
		return restrictToDomain.wildcard();
	}

	@Override
	public boolean loadFromDatabase(long scanRuleId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanRuleLoadFailureException {
		
		// 1 -- Load the scan rule
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		PreparedStatement generalRuleStatement = null;
		ResultSet generalRuleResult = null;
		
		PreparedStatement seedUrlStatement = null;
		ResultSet seedUrlResult = null;
		
		try{
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			// 1.1 -- Load the general rule attributes
			generalRuleStatement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			generalRuleStatement.setLong(1, scanRuleId );
			generalRuleResult = generalRuleStatement.executeQuery();
			
			if( !generalRuleResult.next() ){
				return false;
			}
			
			int scanFrequency = generalRuleResult.getInt("ScanFrequency");
			this.scanFrequency = scanFrequency;
			this.created = generalRuleResult.getTimestamp("Created");
			this.modified = generalRuleResult.getTimestamp("Modified");
			
			// 1.2 -- Load the specific the attributes
			statement = connection.prepareStatement("Select * from HttpDiscoveryRule where ScanRuleID = ?");
			statement.setLong(1, scanRuleId);
			
			resultSet = statement.executeQuery();
			
			if( !resultSet.next() ){
				return false;
			}
			
			this.recursionLevels = resultSet.getInt("RecursionDepth");
			this.restrictToDomain = new Wildcard( resultSet.getString("Domain"), true );
			this.scanCountLimit = resultSet.getInt("ResourceScanLimit");
			this.scanExternalLinks = resultSet.getBoolean("ScanFirstExternal");
			
			this.scanRuleId = scanRuleId;
			
			// 1.3 -- Load the seed URLs
			seedUrlStatement = connection.prepareStatement("Select * from RuleURL where ScanRuleID = ?");
			seedUrlStatement.setLong(1, scanRuleId);
			
			seedUrlResult = seedUrlStatement.executeQuery();
			
			while( seedUrlResult.next() ){
				try{
					addSeedUrl( new URL( seedUrlResult.getString("URL") ) );
				}
				catch(MalformedURLException e){
					throw new ScanRuleLoadFailureException( "One of the seed URLs is invalid: " + seedUrlResult.getString("URL") ,e );
				}
			}

		}
		finally{
			if (statement != null )
				statement.close();
			
			if (resultSet != null )
				resultSet.close();
			
			if (generalRuleStatement != null )
				generalRuleStatement.close();
			
			if (generalRuleResult != null )
				generalRuleResult.close();
			
			if (seedUrlStatement != null )
				seedUrlStatement.close();
			
			if (seedUrlResult != null )
				seedUrlResult.close();
			
			if (connection != null )
				connection.close();
		}
		
		return true;
	}

	private boolean isReady(){
		if(seedUrls.size() == 0){
			return false;
		}
		else{
			return true;
		}
	}
	

	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveToDatabase() throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( scanRuleId == -1 ){
			throw new IllegalStateException("Scan rule must not be less than zero");
		}
		else{
			return saveToDatabaseEx( scanRuleId );
		}
	}
	
	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID. The rule will be associated 
	 * with the sitegroup identified in the argument.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveNewRuleToDatabase( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( siteGroupId < 0 ){
			throw new IllegalArgumentException("Site group identifer must not be less than zero");
		}
		else{
			return saveNewRuleToDatabaseEx( siteGroupId );
		}
	}
	
	/**
	 * Save the rule to the database using the given scan rule ID.
	 * @param scanRuleId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public void saveToDatabase( long scanRuleId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule is valid ( >= 0)
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("Scan rule must not be less than zero");
		
		// 1 -- Save the results
		saveToDatabaseEx( scanRuleId );
	}
	
	/**
	 * Save the rule to the database and associate with the given site group.
	 * @param siteGroupId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveNewRuleToDatabaseEx( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( siteGroupId < -1 )
			throw new IllegalArgumentException("Site group ID is invalid (must not be less than zero)");
		
		//	 0.2 -- Make sure that the rule is ready to be saved (i.e. necessary attributes have been provided)
		if( isReady() == false)
			throw new IllegalStateException("HTTP scan class cannot be persisted to database since critical information is missing");
		
		//	 0.3 -- Make sure a database connection exists
		Connection connection = null;
		
		
		// 2 -- Save the current rule
		PreparedStatement statement = null;
		PreparedStatement saveSeedUrl = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			scanRuleId = createRule( siteGroupId, getScanFrequency(), HttpSeekingScanRule.RULE_TYPE, RULE_STATE_ACTIVE );
			
			statement = connection.prepareStatement("Insert into HttpDiscoveryRule(RecursionDepth, ResourceScanLimit, Domain, ScanFirstExternal, ScanRuleID) values(?, ?, ?, ?, ?)");
			statement.setInt( 1, recursionLevels );
			statement.setInt( 2, scanCountLimit );
			statement.setString( 3, restrictToDomain.wildcard() );
			statement.setBoolean( 4, scanExternalLinks );
			statement.setLong( 5, scanRuleId );
			
			statement.execute();
			
			// 3 -- Save the seed URLs
			Iterator<URL> iterator = seedUrls.iterator();
			while( iterator.hasNext() ){
				saveSeedUrl = connection.prepareStatement("Insert into RuleURL (ScanRuleID, URL) values (?, ?)");
				saveSeedUrl.setLong(1, scanRuleId);
				saveSeedUrl.setString(2, iterator.next().toString());
				
				saveSeedUrl.executeUpdate();
			}
			
			return scanRuleId;
			
		} finally {
			if (saveSeedUrl != null )
				saveSeedUrl.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	
	/**
	 * Save the rule to the database using the given scan rule ID.
	 * @param scanRuleId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveToDatabaseEx( long scanRuleId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("Scan rule ID is invalid (must not be less than zero)");
		
		//	 0.2 -- Make sure that the rule is ready to be saved (i.e. necessary attributes have been provided)
		if( isReady() == false)
			throw new IllegalStateException("HTTP scan class cannot be persisted to database since critical information is missing");
		
		//	 0.3 -- Make sure a database connection exists
		Connection connection = null;
		
		
		// 1 -- Save the current rule
		PreparedStatement statement = null;
		PreparedStatement generalStatement = null;
		PreparedStatement statementDeleteOldSeedUrls = null;
		PreparedStatement saveSeedUrl = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Update HttpDiscoveryRule set RecursionDepth = ?, ResourceScanLimit = ?, Domain = ?, ScanFirstExternal = ? where ScanRuleID = ?");
			statement.setInt( 1, recursionLevels );
			statement.setInt( 2, scanCountLimit );
			statement.setString( 3, restrictToDomain.wildcard() );
			statement.setBoolean( 4, scanExternalLinks );
			statement.setLong( 5, scanRuleId );
			
			statement.executeUpdate();
			
			this.scanRuleId = scanRuleId; //Retain the latest scan rule ID
			
			// 2 -- Save the seed URLs
			
			//		2.1 -- Purge the old seed URLs
			statementDeleteOldSeedUrls = connection.prepareStatement("Delete from RuleURL where ScanRuleID = ?");
			statementDeleteOldSeedUrls.setLong( 1, scanRuleId );
			statementDeleteOldSeedUrls.executeUpdate();
			
			//		2.2 -- Insert the new seed URLs
			Iterator<URL> iterator = seedUrls.iterator();
			while( iterator.hasNext() ){
				
				saveSeedUrl = connection.prepareStatement("Insert into RuleURL (ScanRuleID, URL) values (?, ?)");
				saveSeedUrl.setLong(1, scanRuleId);
				saveSeedUrl.setString(2, iterator.next().toString());
				
				saveSeedUrl.executeUpdate();
			}
			
			// 3 -- Save the generic rule attributes
			generalStatement = connection.prepareStatement("Update ScanRule set ScanFrequency = ?, ScanDataObsolete = ?, Modified = ? where ScanRuleID = ?");
			generalStatement.setInt( 1, this.getScanFrequency() );
			generalStatement.setBoolean( 2, true);
			generalStatement.setTimestamp( 3, new Timestamp(new java.util.Date().getTime()) );
			generalStatement.setLong( 4, scanRuleId );
			generalStatement.executeUpdate();
			
			return this.scanRuleId;
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (saveSeedUrl != null )
				saveSeedUrl.close();
			
			if (generalStatement != null )
				generalStatement.close();
			
			if (statementDeleteOldSeedUrls != null )
				statementDeleteOldSeedUrls.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	@Override
	public ScanResult loadScanResult(long scanResultId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException {
		
		Connection connection = null;
		PreparedStatement statement =null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from ScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			result = statement.executeQuery();
			
			if( !result.next() ){
				return null;//Record was not found
			}
			
			ScanResultCode resultCode = ScanResultCode.getScanResultCodeById(result.getInt("ScanResultCode"));
			
			if( resultCode == null )
				return null;//Result ID is not valid
			
			HttpSeekingScanResult scanResult = HttpSeekingScanResult.loadFromDatabase(result.getLong("ScanRuleID"), scanResultId, resultCode, result.getTimestamp("ScanDate"), result.getInt("Deviations"), result.getInt("Incompletes"), result.getInt("Accepts"));
			
			return scanResult;
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( result != null ){
				result.close();
			}
			
			if( statement != null ){
				statement.close();
			}
		}
	}
	
	/**
	 * Set the limit on the number of the resources the rule will look.
	 * @param maxDepth
	 */
	public final void setScanCountLimit( int resourceLimit ){
		
		// 0 -- Precondition check
		if( resourceLimit > SCAN_LIMIT ){
			throw new IllegalArgumentException("The resource limit cannot exceed " + SCAN_LIMIT);
		}
		
		// 1 -- Set the parameter
		scanCountLimit = resourceLimit;
	}
	
	public int resourcesScanned(){
		return resourcesScanned;
	}
	
	/**
	 * Returns the limit on the number of resources the rule will look at.
	 * @return
	 */
	public final int getScanCountLimit(){
		return scanCountLimit;
	}
	
	/**
	 * Set the number of levels deep the rule will look for links to other resources.
	 * @param depth
	 */
	public final void setRecursionDepth( int depth){
		
		// 0 -- Precondition check
		if( depth > DEPTH_LIMIT ){
			throw new IllegalArgumentException("The depth limit cannot exceed " + DEPTH_LIMIT);
		}
		
		// 1 -- Set the parameter
		recursionLevels = depth;
	}
	
	/**
	 * Returns the recursion depth; the recursion depth is down how many sites the rule will look for other links to resources to scan.
	 * @return
	 */
	public final int getRecursionDepth(){
		return recursionLevels;
	}
	
	/**
	 * Sets the URLs that should be used as the start point for the scan.
	 * @precondition None
	 * @postcondition The URL to be scanned will be saved
	 * @param url The URL to be scanned
	 */
	public final void addSeedUrl(URL url){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the URL is not null
		if( url == null ){
			throw new IllegalArgumentException("A URL to be added to the list cannot be null");
		}
		
		// 1 -- Set the parameter
		seedUrls.add(url);
	}

	/**
	 * Get a list of URLs that do not match the domain restriction.
	 * @return
	 */
	public URL[] getSeedUrlsNotInDomain(){
		
		Iterator<URL> iterators = seedUrls.iterator();
		Vector<URL> urlsNotInDomain = new Vector<URL>();
		
		while( iterators.hasNext() ){
			URL url = iterators.next();
			
			Pattern domainRestriction = restrictToDomain.getPattern();
			Matcher matcher = domainRestriction.matcher( url.toExternalForm() );
			
			if( !matcher.matches() ){
				urlsNotInDomain.add(url);
			}
		}
		
		URL[] urlsArray = new URL[urlsNotInDomain.size()];
		urlsNotInDomain.toArray(urlsArray);
		
		return urlsArray;
	}
	
	/**
	 * Get the URLs that scan is designed to start from.
	 * @return
	 */
	public URL[] getSeedUrls(){
		URL[] startAddresses = new URL[seedUrls.size()];
		seedUrls.toArray(startAddresses);
		
		return startAddresses;
	}
	
	/**
	 * Clear the list of seed URLs.
	 *
	 */
	public void clearSeedUrls(){
		seedUrls.clear();
	}
	
	/**
	 * Set the seed URLS for the rule. This will cause existing seed URLs to be deleted.
	 * @param urls
	 */
	public final void setSeedUrls(URL[] urls){
		seedUrls.clear();
		addSeedUrls(urls);
	}
	
	/**
	 * Baseline the rule against the last scan result.
	 * @return True if the rule successfully baselined.
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws DefinitionSetLoadException
	 * @throws InputValidationException
	 * @throws ScriptException
	 * @throws IOException
	 */
	public synchronized boolean baseline() throws RuleBaselineException, SQLException{
		Connection conn = null;
		
		try{
			// 0 -- Precondition check
			if( this.scanRuleId < 0 ){
				return false;
			}
			
			conn = this.appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			// 1 -- Load the most current scan result
			HttpSeekingScanResult result = (HttpSeekingScanResult)ScanResultLoader.getLastScanResult(this.scanRuleId);
			
			// 2 -- Identify rules to create exceptions for
			
			//	 2.1 -- Don't bother continuing if no scan results exist yet
			if( result == null){
				return false;
			}
			
			HttpDefinitionScanResult[] findings = result.getFindings();
			
			DefinitionPolicySet policySet = DefinitionPolicySet.getPolicySetForSiteGroup(conn, this.scanRuleId);
			
			DefinitionSet definitions = DefinitionArchive.getArchive().getDefinitionSet();
			
			//	 2.2 -- Loop through each finding
			for( HttpDefinitionScanResult finding : findings )
			{
				// Create an entry for each definition match
				for( DefinitionMatch match : finding.getDefinitionMatches() ){
					
						boolean createExceptions = true;
						
						//Find out of the definition is a script and baseline it if is is
						ScriptDefinition def = getScriptDefinition(match.getDefinitionName(), match.getDefinitionID(), definitions);
						
						if ( def != null ){
							finding.ruleId = this.scanRuleId;
							createExceptions = !def.baseline(finding);
						}
						
						if( createExceptions ){
							//Parse out the name (this will be necessary for creating the policy entry)
							String[] name = Definition.parseName(match.getDefinitionName());
						
							String definitionCategory = name[0];
							String definitionSubCategory = name[1];
							String definitionName = name[2];
							
							//If the policy does not already exclude this definition, then create an entry that will
							if( !policySet.isFiltered( this.scanRuleId, definitionName, definitionCategory, definitionSubCategory, finding.getUrl() ) ){
								DefinitionPolicyDescriptor desc = DefinitionPolicyDescriptor.createDefinitionPolicy( (int)this.scanRuleId, match, finding.getUrl(), DefinitionPolicyAction.EXCLUDE );
								desc.saveToDatabase(conn);
							}
						}
				}
				
			}
			
			// 3 -- Set the scan data as obsolete
			ScanRule.setScanDataObsolete(this.scanRuleId);
			
		}
		catch(ScanResultLoadFailureException e){
			return false;
		}
		catch (NoDatabaseConnectionException e) {
			throw new RuleBaselineException("SQL Exception throw while baselining the rule", e);
		} catch (InvalidDefinitionException e) {
			throw new RuleBaselineException("Script could not be baselined (script is invalid)", e);
		} catch (DefinitionEvaluationException e) {
			throw new RuleBaselineException(e);
		} catch (DefinitionSetLoadException e) {
			throw new RuleBaselineException(e);
		} catch (InputValidationException e) {
			throw new RuleBaselineException(e);
		}
		finally{
			if( conn != null ){
				conn.close();
			}
		}
		
		return true;
	}
	
	private ScriptDefinition getScriptDefinition( String name, int localID, DefinitionSet set ){
		Definition def = null;
		
		try{
			def = set.getDefinitionByLocalID(localID);
		}
		catch(NotFoundException e){
			try{
				def = set.getDefinition(name);
			}
			catch(NotFoundException ex){
				return null;
			}
		}
		
		if( def != null && def instanceof ScriptDefinition){
			return (ScriptDefinition)def;
		}
		else{
			return null;
		}
	}
	
	/**
	 * Add all of the URLs in the array.
	 * @param urls
	 */
	public final void addSeedUrls(URL[] urls){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the URL is not null
		if( urls == null ){
			throw new IllegalArgumentException("A URL to be added to the list cannot be null");
		}
		
		// 1 -- Set the parameter
		for( int c = 0; c < urls.length; c++){
			seedUrls.add(urls[c]);
		}
	}
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		if( getStatus() == WorkerThread.State.STOPPED ){
			return 0;
		}
		else{
			return (resourcesScanned * 100)/scanCountLimit;
		}
	}

	public State getStatus() {
		if( inScan && terminate == false ){
			return WorkerThread.State.STARTED;
		}
		else if( inScan && terminate ){
			return WorkerThread.State.STOPPING;
		}
		else if( !inScan ){
			return WorkerThread.State.STOPPED;
		}
		else{
			return WorkerThread.State.STOPPED;
		}
	}

	public String getStatusDescription() {
		WorkerThread.State state = getStatus();
		
		if( state == WorkerThread.State.STARTED){
			if(currentlyScanning == null){
				return "Scan " + getProgress() + "% complete";
			}
			else{
				return "Scan " + getProgress() + "% complete. Currently scanning: " + currentlyScanning;
			}
		}
		else if(state == WorkerThread.State.STOPPED){
			return "Not Scanning";
		}
		else if(state == WorkerThread.State.STOPPING){
			return "Scanner terminating";
		}
		
		return null;
	}

	public String getTaskDescription() {
		return "Website scanner";
	}

	public void pause() {
		//Do nothing, this rule cannot be paused
	}

	public boolean reportsProgress() {
		return true;
	}

	/**
	 * Terminates this rule. Note that partial scan results will be returned (any results from active threads will be ignored).
	 */
	public void terminate() {
		terminate = true;
		CopyOnWriteArrayList<ScanRunner> threads = new CopyOnWriteArrayList<ScanRunner>(runningThreads);
		
		if( threads.size() > 0 ){
			Iterator<ScanRunner> it = threads.iterator();
			while ( it.hasNext() ){
				ScanRunner runner = it.next();
				runner.terminate();
			}
		}
	}

	public HttpSeekingScanResult getResult(){
		return lastScanResult;
	}
	
	public void run() {
		
		try{
			lastScanResult = (HttpSeekingScanResult)doScan();
		}
		catch(ScanException e){
			exceptionThrown = e;
			lastScanResult = null;
		}
	}

	public Throwable getException() {
		return exceptionThrown ;
	}
}

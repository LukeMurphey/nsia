package net.lukeMurphey.nsia.scanRules;

/**
 * The MetaDefinition class is a special type of definition that is used for communicating issues that detected
 * during the processing of data as opposed to the content of the data.
 * @author Luke Murphey
 *
 */
public class MetaDefinition extends Definition {
	
	public static final MetaDefinition REDIRECT_LOOP = new MetaDefinition("RedirectLoop", "Anomaly", "ContentError", 10001, Severity.LOW, Action.ALERT, "An HTTP redirect was detects loops infinitely");
	public static final MetaDefinition BROKEN_LINK = new MetaDefinition( "BrokenLink", "Anomaly", "ContentError", 10002, Severity.LOW, Action.ALERT, "A broken link was detected");
	
	
	public static final MetaDefinition INVALID_SSL_CERTIFICATE = new MetaDefinition("InvalidSSLCertificate", "Anomaly", "ContentError", 10003, Severity.LOW, Action.ALERT, "An invalid SSL certificate was observed");
	public static final MetaDefinition NO_DATA = new MetaDefinition("NoData", "Anomaly", "ContentError", 10004, Severity.LOW, Action.ALERT, "The server failed to return any data");
	public static final MetaDefinition EXPIRED_SSL_CERTIFICATE = new MetaDefinition("OutdatedSSLCertificate", "Anomaly", "ContentError", 10005, Severity.LOW, Action.ALERT, "An outdated SSL certificate was observed");
	public static final MetaDefinition EXCESSIVE_REDIRECTS = new MetaDefinition("ExcessiveRedirects", "Anomaly", "ContentError",  10006, Severity.LOW, Action.ALERT, "An excessive number of HTTP redirects was observed");
	public static final MetaDefinition CONNECTION_FAILED = new MetaDefinition("ConnectionFailure","Anomaly", "ContentError",  10007, Severity.LOW, Action.ALERT, "A connection to the server could not be established");
	public static final MetaDefinition PARSE_FAILURE = new MetaDefinition("ParseFailure", "Anomaly", "ContentError", 10008, Severity.LOW, Action.ALERT, "The content could not be parsed");
	
	public static final MetaDefinition IO_EXCEPTION = new MetaDefinition("IOException", "Anomaly", "ContentError", 10009, Severity.LOW, Action.ALERT, "The content could not be downloaded and evalauted");
	public static final MetaDefinition INVALID_URI = new MetaDefinition("InvalidURI", "Anomaly", "ContentError", 10010, Severity.LOW, Action.ALERT, "The given URI is invalid");
	public static final MetaDefinition HTTP_EXCEPTION = new MetaDefinition("HTTPException", "Anomaly", "ContentError", 10011, Severity.LOW, Action.ALERT, "An HTTP exception occurred while attempting to retrieve the content");
	
	public static final MetaDefinition RESPONSE_CODE_400 = new MetaDefinition("BadRequest", "Anomaly", "ContentError", 10012, Severity.LOW, Action.ALERT, "A 400 (bad request) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_401 = new MetaDefinition("NotAuthorised", "Anomaly", "ContentError", 10013, Severity.LOW, Action.ALERT, "A 401 (not authorised) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_402 = new MetaDefinition("PaymentRequired", "Anomaly", "ContentError", 10014, Severity.LOW, Action.ALERT, "A 402 (payment required) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_403 = new MetaDefinition("Forbidden", "Anomaly", "ContentError", 10015, Severity.LOW, Action.ALERT, "A 403 (forbidden) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_404 = new MetaDefinition("NotFound", "Anomaly", "ContentError", 10016, Severity.LOW, Action.ALERT, "A 404 (not found) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_405 = new MetaDefinition("MethodNotAllowed", "Anomaly", "ContentError", 10017, Severity.LOW, Action.ALERT, "A 405 (method not allowed) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_406 = new MetaDefinition("NotAcceptbable", "Anomaly", "ContentError", 10018, Severity.LOW, Action.ALERT, "A 406 (not acceptbable) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_407 = new MetaDefinition("ProxyAuthRequired", "Anomaly", "ContentError", 10019, Severity.LOW, Action.ALERT, "A 407 (proxy authentication required) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_408 = new MetaDefinition("RequestTimeout", "Anomaly", "ContentError", 10020, Severity.LOW, Action.ALERT, "A 408 (request timeout) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_409 = new MetaDefinition("Conflict", "Anomaly", "ContentError", 10021, Severity.LOW, Action.ALERT, "A 409 (conflict) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_410 = new MetaDefinition("ResourceGone", "Anomaly", "ContentError", 10022, Severity.LOW, Action.ALERT, "A 410 (gone) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_411 = new MetaDefinition("LengthRequired", "Anomaly", "ContentError", 10023, Severity.LOW, Action.ALERT, "A 411 (length required) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_412 = new MetaDefinition("PreconditionFailed", "Anomaly", "ContentError", 10024, Severity.LOW, Action.ALERT, "A 412 (precondition failed) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_413 = new MetaDefinition("EntityTooLarge", "Anomaly", "ContentError", 10025, Severity.LOW, Action.ALERT, "A 413 (request entity too large) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_414 = new MetaDefinition("URITooLong", "Anomaly", "ContentError", 10026, Severity.LOW, Action.ALERT, "A 414 (URI too long) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_415 = new MetaDefinition("UnsupportedMediaType", "Anomaly", "ContentError", 10027, Severity.LOW, Action.ALERT, "A 415 (unsupported media type) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_416 = new MetaDefinition("RequestedRangeUnSatisfiable", "Anomaly", "ContentError", 10028, Severity.LOW, Action.ALERT, "A 416 (requested range not satisifiable) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_417 = new MetaDefinition("ExpectationFailed", "Anomaly", "ContentError", 10029, Severity.LOW, Action.ALERT, "A 417 (exceptation failed) response code was returned");
	
	public static final MetaDefinition RESPONSE_CODE_500 = new MetaDefinition("ServerError", "Anomaly", "ContentError", 10030, Severity.LOW, Action.ALERT, "A 500 (server error) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_501 = new MetaDefinition("NotImplemented", "Anomaly", "ContentError", 10031, Severity.LOW, Action.ALERT, "A 501 (not implemented) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_502 = new MetaDefinition("BadGateway", "Anomaly", "ContentError", 10032, Severity.LOW, Action.ALERT, "A 502 (bad gateway) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_503 = new MetaDefinition("ServiceUnavilable", "Anomaly", "ContentError", 10033, Severity.LOW, Action.ALERT, "A 503 (service unavailable) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_504 = new MetaDefinition("GatewayTimeout", "Anomaly", "ContentError", 10034, Severity.LOW, Action.ALERT, "A 504 (gateway timeout) response code was returned");
	public static final MetaDefinition RESPONSE_CODE_505 = new MetaDefinition("HTTPVersionUnsupported", "Anomaly", "ContentError", 10035, Severity.LOW, Action.ALERT, "A 505 (HTTP version not supported) response code was returned");
	
	public static final MetaDefinition CONNECTION_REFUSED = new MetaDefinition("ConnectionRefused","Anomaly", "ContentError",  10036, Severity.LOW, Action.ALERT, "A connection to the server could not be established (connection refused)");
	public static final MetaDefinition CONNECTION_TIMEOUT = new MetaDefinition("ConnectionTimeout","Anomaly", "ContentError",  10037, Severity.LOW, Action.ALERT, "A connection to the server could not be established (connection timed out)");
	public static final MetaDefinition REDIRECT_LIMIT_EXCEEDED = new MetaDefinition("ExcessiveRedirects", "Anomaly", "ContentError", 10038, Severity.LOW, Action.ALERT, "The page redirected excessively");
	
	public static final MetaDefinition[] DEFAULT_META_DEFINITIONS = new MetaDefinition[]{
		REDIRECT_LOOP, BROKEN_LINK, INVALID_SSL_CERTIFICATE, NO_DATA, EXPIRED_SSL_CERTIFICATE, EXCESSIVE_REDIRECTS,
		CONNECTION_FAILED, PARSE_FAILURE, IO_EXCEPTION, INVALID_URI, HTTP_EXCEPTION, RESPONSE_CODE_400, RESPONSE_CODE_401, RESPONSE_CODE_402,
		RESPONSE_CODE_403, RESPONSE_CODE_404, RESPONSE_CODE_405, RESPONSE_CODE_406, RESPONSE_CODE_407, RESPONSE_CODE_408, RESPONSE_CODE_409,
		RESPONSE_CODE_410, RESPONSE_CODE_411, RESPONSE_CODE_412, RESPONSE_CODE_413, RESPONSE_CODE_414, RESPONSE_CODE_415, RESPONSE_CODE_416,
		RESPONSE_CODE_417, RESPONSE_CODE_500, RESPONSE_CODE_501, RESPONSE_CODE_502, RESPONSE_CODE_503, RESPONSE_CODE_504, RESPONSE_CODE_505,
		REDIRECT_LIMIT_EXCEEDED
	};
	
	public MetaDefinition( String name, String category, String subCategory, int localID, Severity severity, Action action, String defaultMessage ) {
		
		// 0 -- Precondition check
		if(name == null){
			throw new IllegalArgumentException("The definition name must not be null");
		}
		
		if( severity == null ) {
			throw new IllegalArgumentException("The severity must not be null");
		}
		
		if( action == null ) {
			throw new IllegalArgumentException("The action must not be null");
		}
		
		if( defaultMessage == null ) {
			throw new IllegalArgumentException("The default message must not be null");
		}
		
		if( category == null ) {
			throw new IllegalArgumentException("The category must not be null");
		}
		
		if( subCategory == null ) {
			throw new IllegalArgumentException("The sub-category message must not be null");
		}
		
		// 1 -- Initialize the class
		super.action = action;
		super.name = name;
		super.category = category;
		super.subCategory = subCategory;
		super.severity = severity;
		super.localId = localID;
		super.message = defaultMessage;
	}
	
	public MetaDefinition( String name, int localID, Severity severity, Action action, String defaultMessage ) throws InvalidDefinitionException{
		
		// 0 -- Precondition check
		if(name == null){
			throw new IllegalArgumentException("The definition name must not be null");
		}
		
		if( severity == null ) {
			throw new IllegalArgumentException("The severity must not be null");
		}
		
		if( action == null ) {
			throw new IllegalArgumentException("The action must not be null");
		}
		
		if( defaultMessage == null ) {
			throw new IllegalArgumentException("The default message must not be null");
		}
		
		// 1 -- Initialize the class
		super.action = action;
		super.parseFullName(name);
		super.severity = severity;
		super.localId = localID;
		super.message = defaultMessage;
	}
	
	public MetaDefinition createNewWithMessage( String details ){
		return new MetaDefinition(this.name, this.category, this.subCategory, this.localId, this.severity, this.action, this.message + details);
	}
	
}

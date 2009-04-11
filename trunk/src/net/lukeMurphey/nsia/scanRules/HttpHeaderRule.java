package net.lukeMurphey.nsia.scanRules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.regex.*;

import net.lukeMurphey.nsia.Wildcard;

/**
 * 
 * @author luke
 *
 */
public class HttpHeaderRule{
	
	public static final int VALUE_NOT_SET = -1;
	
	// The following constants are used to indicate the type of a rule (string comparison or regular expression matching)
	public static final int RULE_TYPE_REGEX = 0;
	public static final int RULE_TYPE_STRING = 1;
	public static final int RULE_TYPE_WILDCARD = 2;
	
	private Pattern headerNamePattern = null;
	private Pattern headerValuePattern = null;
	private String headerNameString = null;
	private String headerValueString = null;
	private long ruleId = VALUE_NOT_SET;
	private int matchAction = HttpStaticScanRule.MUST_MATCH;
	private int ruleNameType = RULE_TYPE_STRING;
	private int ruleValueType = RULE_TYPE_STRING;
	
	protected HttpHeaderRule(){
		
	}
	
	public HttpHeaderRule( Pattern headerName, Pattern headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( String headerName, Pattern headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}

	public HttpHeaderRule( String headerName, String headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( Wildcard headerName, String headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( Pattern headerName, Wildcard headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( Wildcard headerName, Pattern headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}

	public HttpHeaderRule( Wildcard headerName, Wildcard headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( String headerName, Wildcard headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public HttpHeaderRule( Pattern headerName, String headerValue, int matchAction){
		setHeaderName(headerName);
		setHeaderValue(headerValue);
		setRuleType(matchAction);
	}
	
	public void setHeaderName(String headerName){
		
		// 0 -- Precondition check
		if( headerName == null )
			throw new IllegalArgumentException("Header name cannot be null");
		
		// 1 -- Set the value
		ruleNameType = RULE_TYPE_STRING;
		headerNameString = headerName;
		headerNamePattern = null;
	}
	
	public void setHeaderValue(String headerValue){

		// 0 -- Precondition check
		if( headerValue == null )
			throw new IllegalArgumentException("Header value cannot be null");
		
		// 1 -- Set the value
		ruleValueType = RULE_TYPE_STRING;
		headerValuePattern = null;
		headerValueString = headerValue;
	}
	
	public void setHeaderName(Pattern headerName){
		
		// 0 -- Precondition check
		if( headerName == null )
			throw new IllegalArgumentException("Header name cannot be null");
		
		// 1 -- Set the value
		ruleNameType = RULE_TYPE_REGEX;
		headerNamePattern = headerName;
		headerNameString = null;
	}
	
	public void setHeaderValue(Pattern headerValue){

		// 0 -- Precondition check
		if( headerValue == null )
			throw new IllegalArgumentException("Header value cannot be null");
		
		// 1 -- Set the value
		ruleValueType = RULE_TYPE_REGEX;
		headerValuePattern = headerValue;
		headerValueString = headerValue.pattern();
	}
	
	public void setHeaderName(Wildcard headerName){
		
		// 0 -- Precondition check
		if( headerName == null )
			throw new IllegalArgumentException("Header name cannot be null");
		
		// 1 -- Set the value
		ruleNameType = RULE_TYPE_WILDCARD;
		headerNamePattern = headerName.getPattern();
		headerNameString = headerName.wildcard();
	}
	
	public void setHeaderValue(Wildcard headerValue){

		// 0 -- Precondition check
		if( headerValue == null )
			throw new IllegalArgumentException("Header value cannot be null");
		
		// 1 -- Set the value
		ruleValueType = RULE_TYPE_WILDCARD;
		headerValuePattern = headerValue.getPattern();
		headerValueString = headerValue.wildcard();
	}
	
	public int getNameRuleType(){
		return ruleNameType;
	}
	
	public int getValueRuleType(){
		return ruleValueType;
	}
	
	/*public boolean isHeaderNameRegex(){
		if( headerNamePattern == null )
			return false;
		else 
			return true;
	}
	
	public boolean isHeaderValueRegex(){
		if( headerValuePattern == null )
			return false;
		else 
			return true;
	}*/
	
	public Pattern getHeaderNameRegex(){
		return headerNamePattern;
	}
	
	public Pattern getHeaderValueRegex(){
		return headerValuePattern;
	}
	
	public String getHeaderNameString(){
		return headerNameString;
	}
	
	public String getHeaderValueString(){
		return headerValueString;
	}
	
	public long getRuleId(){
		return ruleId;
	}
	
	private void setRuleId( long newRuleId){
		if( newRuleId < -1)
			throw new IllegalArgumentException("The header rule identifier is invalid");
		
		ruleId = newRuleId;
	}
	
	public boolean isRuleIdSet(){
		if( ruleId >= VALUE_NOT_SET)
			return true;
		else
			return false;
	}
	
	public int getRuleType(){
		return matchAction;
	}
	
	/**
	 * Returns true if the header name given matches this header rule.
	 * @param headerName
	 * @return
	 */
	public boolean doesNameMatch(String headerName){
		
		if( getNameRuleType() == RULE_TYPE_REGEX || getNameRuleType() == RULE_TYPE_WILDCARD ){
			Matcher matcher = headerNamePattern.matcher( headerName );
			
			if( matcher.matches() )
				return true;
		}
		else{
			if( headerNameString.equals(headerName) )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if the header value given matches this header rule.
	 * @param headerName
	 * @return
	 */
	public boolean doesValueMatch(String headerValue){
		
		if( getValueRuleType() == RULE_TYPE_REGEX || getValueRuleType() == RULE_TYPE_WILDCARD ){
			Matcher matcher = headerValuePattern.matcher( headerValue );
			
			if( matcher.matches() )
				return true;
		}
		else{
			if( headerValueString.equals( headerValue ) )
				return true;
		}
		
		return false;
	}
	
	
	/**
	 * Set the rule type (if a rule match causes the rule to reject or accept) 
	 * @param matchAction
	 */
	public void setRuleType(int matchAction){
		if( matchAction != HttpStaticScanRule.MUST_MATCH && matchAction != HttpStaticScanRule.MUST_NOT_MATCH )
			throw new IllegalArgumentException("The match action is not valid");
		
		this.matchAction = matchAction;
	}
	
	
	/**
	 * Save the updated rule to the database 
	 * @param connection
	 * @param scanRuleId
	 * @return
	 * @throws SQLException
	 */
	public long saveToDatabase(Connection connection, long scanRuleId) throws SQLException{
		
		// 0 -- Precondition check
		if( connection == null )
			throw new IllegalArgumentException("The database connection cannot be null");

		
		// 1 -- Save the rule
		PreparedStatement statement = null;
		ResultSet keys = null;
		
		
		try{
			// 1.1a -- Save as a new rule
			if( ruleId == VALUE_NOT_SET ){
				statement = connection.prepareStatement("Insert into HttpHeaderScanRule (ScanRuleID, MatchAction, HeaderName, HeaderNameType, HeaderValue, HeaderValueType) values(?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
				statement.setLong(1, scanRuleId);
				statement.setInt(2, matchAction);
				statement.setString(3, headerNameString);
				statement.setInt(4, ruleNameType);
				statement.setString(5, headerValueString);
				statement.setInt(6, ruleValueType);
				
				if( statement.executeUpdate() < 1 )
					return -1;
				
				keys = statement.getGeneratedKeys();
				
				if( keys.next() ){
					ruleId = keys.getLong(1);
					return ruleId;
				}
				else
					return -1;
				
			}
			// 1.1b -- Save over existing rule
			else{
				statement = connection.prepareStatement("Update HttpHeaderScanRule set MatchAction = ?, HeaderName = ?, HeaderNameType =?, HeaderValue = ?, HeaderValueType = ? where HttpHeaderScanRuleID = ?");
				statement.setInt(1, matchAction);
				statement.setString(2, headerNameString);
				statement.setInt(3, ruleNameType);
				statement.setString(4, headerValueString);
				statement.setInt(5, ruleValueType);
				statement.setLong(6, ruleId);
				
				if( statement.executeUpdate() > 0 )
					return ruleId;
				else
					return -1;
			}
		} finally {
			
			if (statement != null )
				statement.close();
			
			if( keys != null )
				keys.close();
		}

	}
	
	/**
	 * Create a hashtable representation of the rule.
	 * @return
	 */
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		hashtable.put( "Class", getClass().getName() );
		
		hashtable.put("NameRule", headerNameString );
		hashtable.put("NameRuleType", Integer.valueOf( ruleNameType ) );
		
		hashtable.put("ValueRule", headerValueString );
		hashtable.put("ValueRuleType", Integer.valueOf( ruleValueType ) );
		
		hashtable.put("RuleAction", Integer.valueOf(matchAction) );
		hashtable.put("RuleID", Long.valueOf(ruleId) );
		
		return hashtable;
	}
	
	/**
	 * Create a rule from the hashtable.
	 * @return
	 */
	public static HttpHeaderRule getFromHashtable( Hashtable<String, Object> hashtable ){
		
		String className = (String)hashtable.get("Class");
		String nameRule = (String)hashtable.get("NameRule");
		Integer nameRuleType = (Integer)hashtable.get("NameRuleType");
		String valueRule = (String)hashtable.get("ValueRule");
		Integer valueRuleType = (Integer)hashtable.get("ValueRuleType");
		
		Integer ruleId = (Integer)hashtable.get("RuleID");
		Integer ruleType = (Integer)hashtable.get("RuleAction");
		
		if( className == null || !className.matches("net.lukeMurphey.siteSentry.HttpHeaderRule"))
			throw new IllegalArgumentException("Class type invalid");
		
		HttpHeaderRule httpHeaderRule = new HttpHeaderRule();
		
		httpHeaderRule.setRuleType(ruleType.intValue());
		httpHeaderRule.setHeaderName(nameRuleType.intValue(), nameRule);
		httpHeaderRule.setHeaderValue(valueRuleType.intValue(), valueRule);
		
		httpHeaderRule.setRuleId(ruleId.intValue());

		return httpHeaderRule;
	}
	
	/**
	 * Sets the header name to the type specified.
	 * @param type
	 * @param value
	 */
	protected void setHeaderName( int type, String value ){
		if( type == RULE_TYPE_REGEX ){
			setHeaderName(Pattern.compile(value));
		}
		else if( type == RULE_TYPE_STRING ){
			setHeaderName(value);
		}
		else{
			setHeaderName(new Wildcard(value));
		}
	}
	
	/**
	 * Sets the header value to the type specified.
	 * @param type
	 * @param value
	 */
	protected void setHeaderValue( int type, String value ){
		if( type == RULE_TYPE_REGEX ){
			setHeaderValue(Pattern.compile(value));
		}
		else if( type == RULE_TYPE_STRING ){
			setHeaderValue(value);
		}
		else{
			setHeaderValue(new Wildcard(value));
		}
	}
	
	/**
	 * Loads the HTTP Header Rule from the given result set (which presumably points to a tuple that describes
	 * an HTTP header rule).
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	protected static HttpHeaderRule getFromResultSet( ResultSet resultSet ) throws SQLException{
		
		String headerName = resultSet.getString("HeaderName");
		String headerValue = resultSet.getString("HeaderValue");
		int headerValueType = resultSet.getInt("HeaderValueType");
		int headerNameType = resultSet.getInt("HeaderNameType");
		int matchAction = resultSet.getInt("MatchAction");
		long httpHeaderRuleId = resultSet.getInt("HttpHeaderScanRuleID");
		
		HttpHeaderRule headerRule = new HttpHeaderRule();
		
		headerRule.setRuleType(matchAction);
		headerRule.setHeaderName(headerNameType, headerName);
		headerRule.setHeaderValue(headerValueType, headerValue);
		
		headerRule.setRuleId(httpHeaderRuleId);
		
		return headerRule;
	}
}

package net.lukeMurphey.nsia.scanRules;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * The Definition policy represents one entry in the definition exception set.
 * @author luke
 *
 */
public class DefinitionPolicyDescriptor {
	private int siteGroupId = -1;
	private int ruleId = -1;
	private String definitionName;
	private String definitionCategory;
	private String definitionSubCategory;
	private URL url;
	private DefinitionPolicyAction action = DefinitionPolicyAction.EXCLUDE;
	private DefinitionPolicyScope scope; 
	
	private int definitionPolicyEntryId = -1;
	
	public enum DefinitionPolicyAction{
		INCLUDE, EXCLUDE;
		
		public static DefinitionPolicyAction fromInt(int i){
			if( i == INCLUDE.ordinal() ){
				return INCLUDE;
			}
			else if( i == EXCLUDE.ordinal() ){
				return EXCLUDE;
			}
			else{
				return EXCLUDE;
			}
		}
	}
	
	public enum DefinitionPolicyType{
		NAME, CATEGORY, SUBCATEGORY, URL
	}
	
	public enum DefinitionPolicyScope{
		GLOBAL, SITEGROUP, RULE
	}
	
	public DefinitionPolicyDescriptor(int siteGroupId, int ruleId, String definitionName, String definitionCategory, String definitionSubCategory, URL url, DefinitionPolicyAction action){
		this.siteGroupId = siteGroupId;
		this.ruleId = ruleId;
		this.definitionName = definitionName;
		this.definitionCategory = definitionCategory;
		this.definitionSubCategory = definitionSubCategory;
		this.url = url;
		this.action = action;
		
		if( ruleId >= 0 ){
			scope = DefinitionPolicyScope.RULE;
		}
		else if( siteGroupId >= 0 ){
			scope = DefinitionPolicyScope.SITEGROUP;
		}
		else{
			scope = DefinitionPolicyScope.GLOBAL;
		}
	}
	
	public int getSiteGroupID(){
		return siteGroupId;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( String definitionCategory, String definitionSubCategory, String definitionName, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, definitionName, definitionCategory, definitionSubCategory, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( int siteGroupId, int ruleID, String definitionCategory, String definitionSubCategory, String definitionName, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, ruleID, definitionName, definitionCategory, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( int siteGroupId, String definitionCategory, String definitionSubCategory, String definitionName, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, definitionName, definitionCategory, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( int siteGroupId, String definitionCategory, String definitionSubCategory, String definitionName, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, definitionName, definitionCategory, definitionSubCategory, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( int ruleId, DefinitionMatch match, URL url, DefinitionPolicyAction action ){
		
		try{
			String[] name = Definition.parseName(match.getDefinitionName());
			
			String definitionCategory = name[0];
			String definitionSubCategory = name[1];
			String definitionName = name[2];
			
			DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, ruleId, definitionName, definitionCategory, definitionSubCategory, url, action);
			
			return exception;
		}
		catch(InvalidDefinitionException e){
			return null;
		}
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( String definitionCategory, String definitionSubCategory, String definitionName, URL url, DefinitionPolicyAction action, int ruleId ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, ruleId, definitionName, definitionCategory, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( String definitionCategory, String definitionSubCategory, String definitionName, URL url, DefinitionPolicyAction action, int ruleId, int siteGroupID ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupID, ruleId, definitionName, definitionCategory, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionPolicy( String definitionCategory, String definitionSubCategory, String definitionName, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, definitionName, definitionCategory, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( String definitionCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, null, definitionCategory, null, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( int siteGroupId, String definitionCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, null, definitionCategory, null, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( String definitionCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, null, definitionCategory, null, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( int siteGroupId, int ruleID, String definitionCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, ruleID, null, definitionCategory, null, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( int siteGroupId, int ruleID, String definitionCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, ruleID, null, definitionCategory, null, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createCategoryPolicy( int siteGroupId, String definitionCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, null, definitionCategory, null, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy( int siteGroupId, int ruleID, String definitionName, String definitionSubCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, ruleID, definitionName, null, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy( int siteGroupId, String definitionName, String definitionSubCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, definitionName, null, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy( int siteGroupId, int ruleID, String category, String subCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, ruleID, null, category, subCategory, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy( int siteGroupId, String categoryName, String subCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, null, categoryName, subCategory, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createDefinitionNamePolicy( int siteGroupId, String definitionName, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(siteGroupId, -1, definitionName, null, null, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy(  String definitionName, String definitionSubCategory, URL url, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, definitionName, null, definitionSubCategory, url, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSubCategoryPolicy( String categoryName, String subCategory, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, null, categoryName, subCategory, null, action);
		
		return exception;
	}
	
	public static DefinitionPolicyDescriptor createSignatureNamePolicy( String definitionName, DefinitionPolicyAction action ){
		DefinitionPolicyDescriptor exception = new DefinitionPolicyDescriptor(-1, -1, definitionName, null, null, null, action);
		
		return exception;
	}
	
	
	
	private DefinitionPolicyDescriptor(){
		
	}
	
	public DefinitionPolicyAction getAction(){
		return action;
	}
	
	public DefinitionPolicyType getPolicyType(){
		if( definitionName != null ){
			return DefinitionPolicyType.NAME;
		}
		else if( definitionSubCategory != null ){
			return DefinitionPolicyType.SUBCATEGORY;
		}
		else if( definitionCategory != null ){
			return DefinitionPolicyType.CATEGORY;
		}
		else if( url != null ){
			return DefinitionPolicyType.URL;
		}
		else{
			return DefinitionPolicyType.URL;
		}
	}
	
	public DefinitionPolicyScope getPolicyScope(){
		return scope;
	}
	
	public int getPolicyID(){
		return definitionPolicyEntryId;
	}
	
	public int getPolicyRuleID(){
		return ruleId;
	}
	
	public String getDefinitionName(){
		return definitionName;
	}
	
	public String getDefinitionCategory(){
		return definitionCategory;
	}
	
	public String getDefinitionSubCategory(){
		return definitionSubCategory;
	}
	
	public URL getURL(){
		return url;
	}
	
	protected static DefinitionPolicyDescriptor loadFromResult( ResultSet resultSet ) throws SQLException, MalformedURLException{
		
		DefinitionPolicyDescriptor ruleFilter = new DefinitionPolicyDescriptor();
		
		ruleFilter.siteGroupId = defaultIfNull( resultSet, "SiteGroupID", -1);
		ruleFilter.ruleId = defaultIfNull( resultSet, "RuleID", -1);
		ruleFilter.definitionName = resultSet.getString("DefinitionName");
		ruleFilter.definitionCategory = resultSet.getString("DefinitionCategory");
		ruleFilter.definitionSubCategory = resultSet.getString("DefinitionSubCategory");
		ruleFilter.definitionPolicyEntryId = resultSet.getInt("DefinitionPolicyID");
		ruleFilter.action = DefinitionPolicyAction.fromInt( resultSet.getInt("Action") );
		
		String url = resultSet.getString("URL");
		
		if( url != null ){
			ruleFilter.url = new URL(url);
		}
		
		if( ruleFilter.ruleId >= 0 ){
			ruleFilter.scope = DefinitionPolicyScope.RULE;
		}
		else if( ruleFilter.siteGroupId >= 0 ){
			ruleFilter.scope = DefinitionPolicyScope.SITEGROUP;
		}
		else{
			ruleFilter.scope = DefinitionPolicyScope.GLOBAL;
		}
		
		return ruleFilter;
	}
	
	private void addAnd(StringBuffer where, String appendStatement){
		if( where.length() > 0){
			where.append(" AND ");
		}
		
		where.append(appendStatement);
	}
	
	private void purgeIdenticalEntries(Connection connection) throws SQLException{
		
		// 1 -- Create the where clause that will purge any existing items that overlap with the one about to be created
		StringBuffer whereClause = new StringBuffer();
		
		if( siteGroupId >= 0 ){
			addAnd( whereClause, " SiteGroupID = ");
			whereClause.append(siteGroupId);
		}
		else{
			addAnd( whereClause, " (SiteGroupID is null or SiteGroupID = -1)");
		}
		
		if( ruleId >= 0){
			addAnd( whereClause, " RuleID = ");
			whereClause.append(ruleId);
		}
		else{
			addAnd( whereClause, " (RuleID is null or RuleID = -1)");
		}
		
		if( definitionName != null ){
			addAnd( whereClause, " DefinitionName = ");
			whereClause.append( "'" + StringEscapeUtils.escapeSql( definitionName) + "'" );
		}
		else{
			addAnd( whereClause, " DefinitionName is null ");
		}
		
		if( definitionCategory != null ){
			addAnd( whereClause, " DefinitionCategory = ");
			whereClause.append( "'" + StringEscapeUtils.escapeSql( definitionCategory) + "'" );
		}
		else{
			addAnd( whereClause, " DefinitionCategory is null ");
		}
		
		if( definitionSubCategory != null ){
			addAnd( whereClause, " DefinitionSubCategory = ");
			whereClause.append( "'" + StringEscapeUtils.escapeSql( definitionSubCategory) + "'" );
		}
		else{
			addAnd( whereClause, " DefinitionSubCategory is null ");
		}
		
		if( url != null ){
			addAnd( whereClause, " URL = ");
			whereClause.append( "'" + StringEscapeUtils.escapeSql( url.toString() ) + "'" );
		}
		else{
			addAnd( whereClause, " URL is null ");
		}
		
		// 2 -- Execute the statement
		PreparedStatement statement = null;
		
		try{
			statement = connection.prepareStatement("Delete from DefinitionPolicy where " + whereClause.toString() );
			statement.execute();
		}
		finally{
			if( statement != null ){
				statement.close();
			}
		}
		
	}
	
	public void saveToDatabase(Connection connection) throws SQLException{
		
		PreparedStatement statement = null;
		ResultSet keys = null;

		// This is a new entry, add it to the database
		try{
			connection.setAutoCommit(false);
			purgeIdenticalEntries(connection);
			
			if(definitionPolicyEntryId < 0 ){

				statement = connection.prepareStatement("Insert into DefinitionPolicy (SiteGroupID, RuleID, DefinitionName, DefinitionCategory, DefinitionSubCategory, URL, Action) values (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

				statement.setLong(1, siteGroupId);
				statement.setLong(2, ruleId);
				
				if( definitionName == null ){
					statement.setNull(3, java.sql.Types.VARCHAR);//statement.setObject(3, null);
				}
				else{
					statement.setString(3, definitionName);
				}
				
				if( definitionCategory == null ){
					statement.setNull(4, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(4, definitionCategory);
				}
				
				if( definitionSubCategory == null ){
					statement.setNull(5, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(5, definitionSubCategory);
				}
				
				if( url == null ){
					statement.setNull(6, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(6, url.toString());
				}

				statement.setInt(7, action.ordinal());
				
				statement.executeUpdate();
				keys = statement.getGeneratedKeys();

				if( keys.next() ){
					definitionPolicyEntryId = keys.getInt(1);
				}
			}
			else
			{
				statement = connection.prepareStatement("Update DefinitionPolicy set (SiteGroupID = ?, DefinitionID = ?, DefinitionName= ?, DefinitionCategory= ?, DefinitionSubCategory= ?, URL= ?, Action = ?) where DefinitionPolicyID = ?", Statement.RETURN_GENERATED_KEYS);

				statement.setLong(1, siteGroupId);
				statement.setLong(2, ruleId);
				
				if( definitionName == null ){
					statement.setNull(3, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(3, definitionName);
				}
				
				if( definitionCategory == null ){
					statement.setNull(4, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(4, definitionCategory);
				}
				
				if( definitionSubCategory == null ){
					statement.setNull(5, java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(5, definitionSubCategory);
				}
				
				if( url == null ){
					statement.setNull(5,java.sql.Types.VARCHAR);
				}
				else{
					statement.setString(5, url.toString());
				}
				
				statement.setInt(6, action.ordinal());
				statement.setInt(7, definitionPolicyEntryId);

				statement.executeUpdate();
			}
			
			connection.commit();
		}
		finally{
			connection.setAutoCommit(true);
			
			if( statement != null){
				statement.close();
			}
			
			if( keys != null){
				keys.close();
			}
		}
	}
	
	private static int defaultIfNull(ResultSet resultSet, String columnName, int defaultValue) throws SQLException{
		int value = resultSet.getInt(columnName);
		
		if( resultSet.wasNull() ){
			value = defaultValue;
		}
		
		return value;
	}
	
	public DefinitionPolicyAction getActionIfMatches (long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, String url){
		return matchesInternal(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, url);
	}
	
	public DefinitionPolicyAction getActionIfMatches(long siteGroupId, int ruleId, String ruleName, String ruleCategory, String ruleSubCategory){
		return matchesInternal(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, null);
	}
	
	private DefinitionPolicyAction matchesInternal(long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, String url){
		
		// 1 -- Site group identifier is specified in filter and does not match
		if( this.siteGroupId > 0 && this.siteGroupId != siteGroupId){
			return null;
		}
		// 2 -- Rule identifier is specified in filter and does not match
		else if( this.ruleId > 0 && this.ruleId != ruleId){
			return null;
		}
		// 3 -- Rule name identifier is specified in filter and does not match
		else if( this.definitionName != null && !this.definitionName.equalsIgnoreCase(ruleName) ){
			return null;
		}
		// 4 -- Rule category identifier is specified in filter and does not match
		else if( this.definitionCategory != null && !this.definitionCategory.equalsIgnoreCase(ruleCategory) ){
			return null;
		}
		// 5 -- Rule sub-category identifier is specified in filter and does not match
		else if( this.definitionSubCategory != null && !this.definitionSubCategory.equalsIgnoreCase(ruleSubCategory) ){
			return null;
		}
		// 6 -- URL is specified in filter and does not match
		else if( this.url != null && !this.url.toString().equalsIgnoreCase(url)){
			return null;
		}
		// 7 -- Otherwise, the filter matches
		else{
			return action;
		}
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		if( definitionCategory != null ){
			buffer.append(definitionCategory);
		}
		
		if( definitionSubCategory != null ){
			buffer.append(".");
			buffer.append(definitionSubCategory);
		}
		
		if( definitionName != null ){
			buffer.append(".");
			buffer.append(definitionName);
		}
		
		if( url != null ){
			if( buffer.length() > 0 ){
				buffer.append(" for ");
			}
			
			buffer.append(url);
		}
		
		return buffer.toString();
	}
	
	public DefinitionPolicyAction getActionIfMatches(long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, URL url){
		if( url != null){
			return matchesInternal(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, url.toString());
		}
		else{
			return matchesInternal(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, null);
		}
	}
	
}

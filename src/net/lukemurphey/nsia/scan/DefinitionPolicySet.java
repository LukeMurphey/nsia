package net.lukemurphey.nsia.scan;

import java.net.*;
import java.sql.*;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyScope;

/**
 * The DefinitionPolicySet allows rules to be filtered for certain resources (filtered for an entire Site Group, definition, definition category, etc.)
 * @author luke
 *
 */
public class DefinitionPolicySet {
	
	private Vector<DefinitionPolicyDescriptor> definitionPolicies = new Vector<DefinitionPolicyDescriptor>();
	
	private DefinitionPolicySet(){
		
	}
	
	public DefinitionPolicySet( Vector<DefinitionPolicyDescriptor> descriptors){
		definitionPolicies.addAll(descriptors);
		sort();
	}
	
	public DefinitionPolicySet( DefinitionPolicyDescriptor... descriptors){
		for( DefinitionPolicyDescriptor descriptor : descriptors){
			if( descriptor != null ){
				definitionPolicies.add(descriptor);
			}
		}
		
		sort();
	}
	
	public static DefinitionPolicySet getPolicySetForRule(Connection connection, int ruleID) throws SQLException{
		PreparedStatement statement = null;
		ResultSet result = null;
		DefinitionPolicySet filterSet = new DefinitionPolicySet();
		
		try{
			statement = connection.prepareStatement("Select * from DefinitionPolicy where RuleID = ?");
			statement.setInt(1, ruleID);
			
			result = statement.executeQuery();
			
			while(result.next()){
				try {
					filterSet.definitionPolicies.add( DefinitionPolicyDescriptor.loadFromResult(result) );
				} catch (MalformedURLException e) {
					Application.getApplication().logExceptionEvent(new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Invalid URL was observed when loading definition policy")), e);
				}
			}
			
		}
		finally{
			if(statement != null ){
				statement.close();
			}
			
			if(result != null ){
				result.close();
			}
		}
		
		filterSet.sort();
		return filterSet;
	}
	
	public static DefinitionPolicySet getPolicySetForSiteGroup(Connection connection, int siteGroupId, int ruleID) throws SQLException{
		PreparedStatement statement = null;
		ResultSet result = null;
		DefinitionPolicySet filterSet = new DefinitionPolicySet();
		
		try{
			statement = connection.prepareStatement("Select * from DefinitionPolicy where (SiteGroupID is null or SiteGroupID = ? or SiteGroupID = -1) and RuleID = ?");
			statement.setInt(1, siteGroupId);
			statement.setInt(2, ruleID);
			
			result = statement.executeQuery();
			
			while(result.next()){
				try {
					filterSet.definitionPolicies.add( DefinitionPolicyDescriptor.loadFromResult(result) );
				} catch (MalformedURLException e) {
					Application.getApplication().logExceptionEvent(new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Invalid URL was observed when loading definition policy")), e);
				}
			}
			
		}
		finally{
			if(statement != null ){
				statement.close();
			}
			
			if(result != null ){
				result.close();
			}
		}
		
		filterSet.sort();
		return filterSet;
	}
	
	public static DefinitionPolicySet getPolicySetForSiteGroup(Connection connection, long siteGroupId) throws SQLException{
		PreparedStatement statement = null;
		ResultSet result = null;
		DefinitionPolicySet filterSet = new DefinitionPolicySet();
		
		try{
			statement = connection.prepareStatement("Select * from DefinitionPolicy where SiteGroupID is null or SiteGroupID = -1 or SiteGroupID = ?");
			statement.setLong(1, siteGroupId);
			
			result = statement.executeQuery();
			
			while(result.next()){
				try {
					filterSet.definitionPolicies.add( DefinitionPolicyDescriptor.loadFromResult(result) );
				} catch (MalformedURLException e) {
					Application.getApplication().logExceptionEvent(new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Invalid URL was observed when loading definition policy")), e);
				}
			}
			
		}
		finally{
			if(statement != null ){
				statement.close();
			}
			
			if(result != null ){
				result.close();
			}
		}
		
		filterSet.sort();
		return filterSet;
	}
	
	private void sort(){
		Collections.sort(definitionPolicies, new DefinitionPolicyComparator() );
	}
	
	public static DefinitionPolicySet getPolicySet(Connection connection) throws SQLException{
		PreparedStatement statement = null;
		ResultSet result = null;
		DefinitionPolicySet filterSet = new DefinitionPolicySet();
		
		try{
			statement = connection.prepareStatement("Select * from DefinitionPolicy");
			
			result = statement.executeQuery();
			
			while(result.next()){
				try {
					filterSet.definitionPolicies.add( DefinitionPolicyDescriptor.loadFromResult(result) );
				} catch (MalformedURLException e) {
					Application.getApplication().logExceptionEvent(new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Invalid URL was observed when loading definition policy")), e);
				}
			}
			
		}
		finally{
			if(statement != null ){
				statement.close();
			}
			
			if(result != null ){
				result.close();
			}
		}
		
		filterSet.sort();
		return filterSet;
	}
	
	/**
	 * This comparator is used to sort the definition according to the policy scope. This is done so that the list can be search easily to
	 * find the most specific policy descriptor. By ordering them, one can stop searching the list as soon as a matching policy is found.
	 * Note that policies that are more specific override those that are less specific. For example, a policy defined for a site-group can
	 * override a policy defined at the global level. By ordering all site-group policies above global policies, the search will always see
	 * the site-group policies first. If a site-group policy matches, then the global policy will never be found (and thus is overridden).  
	 * @author Luke Murphey
	 *
	 */
	private static class DefinitionPolicyComparator implements java.util.Comparator<DefinitionPolicyDescriptor> {

		public int compare(DefinitionPolicyDescriptor first, DefinitionPolicyDescriptor second) {
			
			if( first == null ){
				return 1;
			}
			else if(second == null){
				return -1;
			}
			else if( first.getPolicyScope() == second.getPolicyScope() ){
				return 0;
			}
			else if( first.getPolicyScope() == DefinitionPolicyScope.RULE ){
				return -1;
			}
			else if( second.getPolicyScope() == DefinitionPolicyScope.RULE ){
				return 1;
			}
			else if( first.getPolicyScope() == DefinitionPolicyScope.SITEGROUP ){
				return -1;
			}
			else if( second.getPolicyScope() == DefinitionPolicyScope.SITEGROUP ){
				return 1;
			}
			else if( first.getPolicyScope() == DefinitionPolicyScope.GLOBAL ){
				return -1;
			}
			else if( second.getPolicyScope() == DefinitionPolicyScope.GLOBAL ){
				return 1;
			}
			
			return 0;
		}
		
	}
	
	/**
	 * Get the list of filters in an array.
	 * @return
	 */
	public DefinitionPolicyDescriptor[] getRuleFiltersArray(){
		DefinitionPolicyDescriptor[] filters = new DefinitionPolicyDescriptor[definitionPolicies.size()];
		
		definitionPolicies.toArray(filters);
		
		return filters;
	}
	
	/**
	 * Get the rule filter at the given index.
	 * @return
	 */
	public DefinitionPolicyDescriptor get(int c){
		return definitionPolicies.get(c);
	}
	
	/**
	 * Get the number of rule filters.
	 * @return
	 */
	public int size(){
		return definitionPolicies.size();
	}
	
	/**
	 * Get the number of policies that exclude definitions.
	 * @return
	 */
	public int getExcludePolicyCount(){
		return getPolicyTypeCount( DefinitionPolicyAction.EXCLUDE );
	}
	
	/**
	 * Get the number of policies that include definitions.
	 * @return
	 */
	public int getIncludePolicyCount(){
		return getPolicyTypeCount( DefinitionPolicyAction.INCLUDE );
	}
	
	/**
	 * Count the number of policies for the given policy action type.
	 * @param action
	 * @return
	 */
	public int getPolicyTypeCount( DefinitionPolicyAction action ){
		int c = 0;
		for (DefinitionPolicyDescriptor policy : definitionPolicies) {
			if( policy.getAction() == action ){
				c++;
			}
		}
		
		return c;
	}
	
	/**
	 * Gets the policy that matches the parameters given.
	 * @param siteGroupId
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public DefinitionPolicyDescriptor getMatchingPolicy(long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, String url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, url);
			if( action != null){
				return definitionPolicies.get(c);
			}
		}
		
		return null;
	}
	
	/**
	 * Determine if the filter set includes an entry to filter the definition described by the given attributes.  
	 * @param siteGroupId
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public DefinitionPolicyAction getPolicyAction(long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, String url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, url);
			if( action != null){
				return action;
			}
		}
		
		return null;
	}
	
	/**
	 * Determine if the policy includes the given definition. If false is returned, then the definition should be ignored.
	 * @param ruleId
	 * @param definition
	 * @param url
	 * @return
	 */
	public boolean isFiltered( long ruleId, Definition definition, String url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(-1, ruleId, definition.getName(), definition.getCategoryName(), definition.getSubCategoryName(), url);
			
			if(action == null ){
				//Fall through
			}
			else if( action == DefinitionPolicyAction.INCLUDE ){
				return false;
			}
			else if( action == DefinitionPolicyAction.EXCLUDE ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determine if the policy includes the given definition. If false is returned, then the definition should be ignored.  
	 * @param siteGroupId
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public boolean isFiltered(long siteGroupId, long ruleId, Definition definition, String url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(siteGroupId, ruleId, definition.getName(), definition.getCategoryName(), definition.getSubCategoryName(), url);
			
			if(action == null ){
				//Fall through
			}
			else if( action == DefinitionPolicyAction.INCLUDE ){
				return false;
			}
			else if( action == DefinitionPolicyAction.EXCLUDE ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determine if the filter set includes an entry to filter the definition described by the given attributes.  
	 * @param siteGroupId
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public boolean isFiltered(long siteGroupId, long ruleId, Definition definition, URL url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(siteGroupId, ruleId, definition.getName(), definition.getCategoryName(), definition.getSubCategoryName(), url);
			
			if(action == null ){
				//Fall through
			}
			else if( action == DefinitionPolicyAction.INCLUDE ){
				return false;
			}
			else if( action == DefinitionPolicyAction.EXCLUDE ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determine if the filter set includes an entry to filter the definition described by the given attributes.  
	 * @param siteGroupId
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public boolean isFiltered(long siteGroupId, long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, URL url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(siteGroupId, ruleId, ruleName, ruleCategory, ruleSubCategory, url);
			
			if(action == null ){
				//Fall through
			}
			else if( action == DefinitionPolicyAction.INCLUDE ){
				return false;
			}
			else if( action == DefinitionPolicyAction.EXCLUDE ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determine if the filter set includes an entry to filter the definition described by the given attributes.  
	 * @param ruleId
	 * @param ruleName
	 * @param ruleCategory
	 * @param ruleSubCategory
	 * @param url
	 * @return
	 */
	public boolean isFiltered( long ruleId, String ruleName, String ruleCategory, String ruleSubCategory, URL url){
		
		for(int c = 0; c < definitionPolicies.size(); c++){
			DefinitionPolicyAction action = definitionPolicies.get(c).getActionIfMatches(-1, ruleId, ruleName, ruleCategory, ruleSubCategory, url);
			
			if(action == null ){
				//Fall through
			}
			else if( action == DefinitionPolicyAction.INCLUDE ){
				return false;
			}
			else if( action == DefinitionPolicyAction.EXCLUDE ){
				return true;
			}
		}
		
		return false;
	}
	
}


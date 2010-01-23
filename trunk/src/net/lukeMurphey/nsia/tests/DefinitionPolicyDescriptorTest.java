package net.lukemurphey.nsia.tests;

import java.util.Vector;

import junit.framework.TestCase;
import net.lukemurphey.nsia.scan.*;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;

public class DefinitionPolicyDescriptorTest extends TestCase {
	
	/*
	public void testSave() throws BindException, SQLException, NoDatabaseConnectionException, InputValidationException, Exception{
		RuleFilter filter = new RuleFilter(1, -1, null, null, null, null);
		
		filter.saveToDatabase(TestsConfig.getApplicationResource().getDatabaseConnection(DatabaseAccessType.SCANNER));
	}
	*/
	
	public void testFilterMultipleOverride(){
		Vector<DefinitionPolicyDescriptor> descriptors = new Vector<DefinitionPolicyDescriptor>();
		
		// Entry 1
		descriptors.add( DefinitionPolicyDescriptor.createCategoryPolicy("CategoryOne", DefinitionPolicyAction.EXCLUDE) );
		
		// Entry 2
		descriptors.add( DefinitionPolicyDescriptor.createCategoryPolicy(1, "CategoryOne", DefinitionPolicyAction.INCLUDE) );
		
		// Entry 3
		descriptors.add( DefinitionPolicyDescriptor.createSubCategoryPolicy(1, "SubCategory", "CategoryTwo", DefinitionPolicyAction.EXCLUDE) );
		
		// Entry 4
		descriptors.add( DefinitionPolicyDescriptor.createCategoryPolicy(2, "CategoryOne", DefinitionPolicyAction.EXCLUDE) );
		
		// Entry 5
		descriptors.add( DefinitionPolicyDescriptor.createDefinitionPolicy(-1, "CategoryOne", "SubCategory", "FilterMe", DefinitionPolicyAction.EXCLUDE) );
		
		DefinitionPolicySet set = new DefinitionPolicySet(descriptors);
		
		//This one should be accepted by entry[2]
		if( set.isFiltered(1, -1, "DefName", "CategoryOne", "SubCategory", null)  ){
			fail("The isFiltered method should have returned false");
		}
		
		// This one should be rejected by entry[4]
		if( set.isFiltered(2, -1, "DefName", "CategoryOne", "SubCategory", null) == false ){
			fail("The isFiltered method should have returned true");
		}
		
		// This one should be rejected by entry[1]
		if( set.isFiltered(32, -1, "DefName", "CategoryOne", "SubCategory", null) == false ){
			fail("The isFiltered method should have returned true");
		}
		
		// This one should be rejected by entry[5]
		if( set.isFiltered(5, -1, "FilterMe", "CategoryOne", "SubCategory", null) == false ){
			fail("The isFiltered method should have returned true");
		}
		
	}
	
	public void testFilterSiteGroupID(){
		DefinitionPolicyDescriptor filter = new DefinitionPolicyDescriptor(1, -1, null, null, null, null, DefinitionPolicyAction.EXCLUDE);
		
		if( DefinitionPolicyAction.EXCLUDE != filter.getActionIfMatches(1, -1, null, null, null) ){
			fail("The isFiltered method should have returned EXCLUDE");
		}
		
		DefinitionPolicyDescriptor filter2 = new DefinitionPolicyDescriptor(2, -1, null, null, null, null, DefinitionPolicyAction.EXCLUDE);
		
		if( null != filter2.getActionIfMatches(1, -1, null, null, null) ){
			fail("The isFiltered method should have returned null");
		}
		
	}
	
	public void testFilterSiteGroupRule(){
		DefinitionPolicyDescriptor filter = new DefinitionPolicyDescriptor(1, 1024, null, null, null, null, DefinitionPolicyAction.EXCLUDE);
		
		if( DefinitionPolicyAction.EXCLUDE != filter.getActionIfMatches(1, 1024, null, null, null) ){
			fail("The isFiltered method should have returned EXCLUDE");
		}
		
		if( null != filter.getActionIfMatches(1, -1, null, null, null) ){
			fail("The isFiltered method should have returned null");
		}
		
		if( null != filter.getActionIfMatches(1, 2148, null, null, null) ){
			fail("The isFiltered method should have returned null");
		}
		
		if( null != filter.getActionIfMatches(-1, 2148, null, null, null) ){
			fail("The isFiltered method should have returned null");
		}
		
		if( null != filter.getActionIfMatches(1, -1, "tree", null, null) ){
			fail("The isFiltered method should have returned null");
		}
		
	}

}

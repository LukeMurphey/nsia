package net.lukemurphey.nsia.htmlInterface;

/**
 * The following class represents the result of an action performed by one of the HTML generation classes.
 * @author luke
 *
 */
public class ActionDescriptor {

	protected static final int OP_NO_OPERATION = -1;
	protected static final int OP_DELETE_SUCCESS = 0;
	protected static final int OP_DELETE_FAILED = 1;
	protected static final int OP_ADD_SUCCESS = 2;
	protected static final int OP_ADD_FAILED = 3;
	protected static final int OP_UPDATE_SUCCESS = 4;
	protected static final int OP_UPDATE_FAILED = 5;
	protected static final int OP_DELETE = 6;
	protected static final int OP_ADD = 7;
	protected static final int OP_UPDATE = 8;
	protected static final int OP_LIST = 9;
	protected static final int OP_ENABLE_SUCCESS = 10;
	protected static final int OP_ENABLE_FAILED = 11;
	protected static final int OP_DISABLE_SUCCESS = 12;
	protected static final int OP_DISABLE_FAILED = 13;
	protected static final int OP_VIEW = 14;

	public Object addData;
	public int result;
	
	
	
	public ActionDescriptor( int resultCode ){
		result = resultCode;
	}
	
	public ActionDescriptor( int resultCode, Object additionalData ){
		result = resultCode;
		addData = additionalData;
	}

}

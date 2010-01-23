package net.lukemurphey.nsia.htmlInterface;

public class InvalidHtmlParameterException extends Exception{
	
	private static final long serialVersionUID = -4284295963405474525L;
	private String _issueTitle;
	private String _issueDescription;
	private String _alternativeUrl;
	
	InvalidHtmlParameterException( String issueTitle, String issueDescription, String alternativeUrl ){
		
		_issueTitle = issueTitle;
		_issueDescription = issueDescription;
		_alternativeUrl = alternativeUrl;
	}
	
	public String getIssueTitle(){
		return _issueTitle;
	}
	
	public String getIssueDescription(){
		return _issueDescription;
	}
	
	public String getAlternativeUrl(){
		return _alternativeUrl;
	}

}

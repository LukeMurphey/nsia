package net.lukeMurphey.nsia.htmlInterface;

public class InvalidHtmlOperationException extends Exception{

	private static final long serialVersionUID = 6842766183566364187L;
	private String _issueTitle;
	private String _issueDescription;
	private String _alternativeUrl;
	
	InvalidHtmlOperationException( String issueTitle, String issueDescription, String alternativeUrl ){
		
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

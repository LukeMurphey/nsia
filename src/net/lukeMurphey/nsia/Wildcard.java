package net.lukemurphey.nsia;

import java.util.regex.*;

public class Wildcard {
	
	protected Pattern pattern = null;
	protected String wildcard = null;
	
	public Wildcard(String wildcardPattern){
		wildcard = wildcardPattern;
		pattern = Wildcard.compile( wildcardPattern, false );
	}
	
	public Wildcard(String wildcardPattern, boolean caseInsensitive){
		wildcard = wildcardPattern;
		
		pattern = Wildcard.compile( wildcardPattern, caseInsensitive );
	}
	
	public String wildcard(){
		return wildcard;
	}
	
	public Pattern getPattern(){
		return pattern;
	}
	
    private static Pattern compile( String wildcard, boolean caseInsensitive ){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        
        if( caseInsensitive){
        	return Pattern.compile(s.toString(), Pattern.CASE_INSENSITIVE);
        }
        else{
        	return Pattern.compile(s.toString());
        }
    }

}

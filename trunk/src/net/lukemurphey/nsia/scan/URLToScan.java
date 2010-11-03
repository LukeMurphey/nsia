package net.lukemurphey.nsia.scan;

import java.net.URL;

/**
 * This class represents a URL that needs to be scanned. This is intended to be used by definition that identify URLs that ought to be scanned and it allows the definitions
 * to define whether the URL ought to be scanned even if it does not match the domain. This is useful to distinguish between URLs for resources that are automatically
 * loaded by web-browers (like images) and those that must be clicked to be seen.
 * @author Luke
 *
 */
public class URLToScan {
	
	public static final boolean IGNORE_DOMAIN_RESTRICTION_DEFAULT = false;
	
	private boolean ignoreDomainRestriction = IGNORE_DOMAIN_RESTRICTION_DEFAULT;
	private URL url = null;
	
	public URLToScan( URL url ){
		if (url != null ){
			this.url = url;
		}
	}
	
	public URLToScan( URL url, boolean ignoreDomainRestriction ){
		if (url != null ){
			this.url = url;
		}
		
		this.ignoreDomainRestriction = ignoreDomainRestriction;
	}
	
	public URL getURL(){
		return url;
	}
	
	public boolean ignoreDomainRestriction(){
		return ignoreDomainRestriction;
	}

}

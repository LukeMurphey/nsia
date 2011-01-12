package net.lukemurphey.nsia.scan.scriptenvironment;

import net.lukemurphey.nsia.scan.HttpResponseData;

import org.jsoup.*;
import org.jsoup.nodes.Document;

public class JSoup {

	public static Document parse ( HttpResponseData response ){
		return Jsoup.parse( response.getResponseAsString() );
	}
	
}

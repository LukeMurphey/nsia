package net.lukemurphey.nsia;

import javax.net.ssl.*;
import java.security.cert.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class ThreatFactorTrustManager implements X509TrustManager {

	public static void register() throws NoSuchAlgorithmException, KeyManagementException{
		TrustManager[] myTMs = new TrustManager [] { new ThreatFactorTrustManager() };
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, myTMs, null);
	}

	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		// TODO Finish trust manager class
		System.out.println("checkClientTrusted");
	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		
		System.out.println("checkServerTrusted");
	}

	public X509Certificate[] getAcceptedIssuers() {
		
		System.out.println("getAcceptedIssuers");
		return null;
	}
	
}

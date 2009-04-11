package net.lukeMurphey.nsia;

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

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		// TODO Finish trust manager class
		System.out.println("checkClientTrusted");
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		
		System.out.println("checkServerTrusted");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		
		System.out.println("getAcceptedIssuers");
		return null;
	}
	
}

/**
 * 
 */
package org.jirafe.test.occ.util;

/**
 * @author hapham1
 *
 */
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


public class DummyTrustManager implements X509TrustManager
{

	@Override
	public void checkClientTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException
	{
		//intent to be blank
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException
	{
		//intent to be blank
	}

	@Override
	public X509Certificate[] getAcceptedIssuers()
	{
		return null;
	}
}
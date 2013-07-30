package org.jirafe.test.occ.util;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


public class DummyHostnameVerifier implements HostnameVerifier
{
	@Override
	public boolean verify(final String hostname, final SSLSession session)
	{
		return true;
	}
}
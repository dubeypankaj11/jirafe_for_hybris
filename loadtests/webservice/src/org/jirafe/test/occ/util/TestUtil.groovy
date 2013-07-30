/**
 *
 */
package org.jirafe.test.occ.util

/**
 * @author hapham1
 *
 */
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.X509TrustManager

import groovy.json.*


class TestUtil {

	//static String HOST = 'localhost'
	static String HOST = 'staging-hybris487-01.int.jirafe.net'
	static String BASE = "http://${HOST}:9001/rest/v1/electronics"
	static String SECURE_BASE = "https://${HOST}:9002/rest/v1/electronics"

	static String OAUTH2_TOKEN_ENDPOINT = "https://${HOST}:9002/rest/oauth/token"

	static String CLIENT_ID = 'mobile_android'
	static String CLIENT_SECRET = 'secret'

	//	static String BASE = 'http://qa-hybris-487-01.int.jirafe.net:9001/rest/v1/electronics'
	//	static String SECURE_BASE = 'https://qa-hybris-487-01.int.jirafe.net:9002/rest/v1/electronics'

	static String USERNAME = 'demo'
	static String PASSWORD = '1234'

	static getConnection(path, method) {
		def url = BASE + path
		def con = url.toURL().openConnection()

		if (method == "POST")
			con.doOutput = true

		con.requestMethod = method
		return con
	}

	static getURLConnection(url, method) {
		def con = url.toURL().openConnection()
		con.requestMethod = method
		return con
	}

	static getSecureURLConnection(url, method) {
		fakeSecurity()
		def con = url.toURL().openConnection()
		con.requestMethod = method
		return con
	}

	static getSecureConnection(path, method) {
		fakeSecurity()
		def url = SECURE_BASE + path
		def con = url.toURL().openConnection()

		if (method == "POST")
			con.doOutput = true

		con.requestMethod = method
		return con
	}

	static fakeSecurity() {
		def trustManager = new DummyTrustManager()
		def hostnameVerifier = new DummyHostnameVerifier();
		def sc = javax.net.ssl.SSLContext.getInstance("SSL");
		sc.init(null, [trustManager] as X509TrustManager[], new java.security.SecureRandom());
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
	}

	static messageResponseCode(returned, expected) {
		return "Response Code is: " + returned + ", expected: " + expected
	}

	static basicAuth(con) {
		String userpassword = TestUtil.USERNAME + ":" + TestUtil.PASSWORD;
		String encodedAuthorization = userpassword.bytes.encodeBase64().toString()
		con.setRequestProperty("Authorization", "Basic " + encodedAuthorization)
	}

	static basicAuth(con, username, password) {
		String userpassword = username + ":" + password;
		String encodedAuthorization = userpassword.bytes.encodeBase64().toString()

		assert con instanceof HttpsURLConnection : "Basic Auth always requires HTTPS!"

		con.setRequestProperty("Authorization", "Basic " + encodedAuthorization)
	}

	/************************
	 Get client's access token by sending an authorization call
	 https://<host>:<port>/rest/oauth/token?client_id=$\{clientId\}&client_secret=$\{clientSecret\}&grant_type=client_credentials
	 *************************/
	static oAuth_getClientAccessToken() {
		fakeSecurity()

		def url = OAUTH2_TOKEN_ENDPOINT
		def auth_con = url.toURL().openConnection()
		auth_con.doOutput = true
		auth_con.requestMethod = "POST"

		String clientId = TestUtil.CLIENT_ID
		String clientSecret = TestUtil.CLIENT_SECRET
		auth_con.outputStream << "client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials"

		def body
		if (auth_con.responseCode != HttpURLConnection.HTTP_OK ||
				!(body = auth_con.content.text) || body == "") {
			println "\n\nOAuth request failed!, falling back to basic auth"
			return null
		}
		def result = new JsonSlurper().parseText(body)
		return result.access_token
	}

	/**
	 * Get user's access token by sending an authorization call
	 * https://<host>:<port>/rest/oauth/token?client_id=$CLIENT_ID$&client_secret=$CLIENT_SECRET$&grant_type=password&username=$username$&password=$password$
	 *
	 * @param username
	 * @param password
	 * @return
	 */

	static oAuth_getUserAccessToken(username, password) {
		fakeSecurity()

		def url = OAUTH2_TOKEN_ENDPOINT
		def auth_con = url.toURL().openConnection()
		auth_con.doOutput = true
		auth_con.requestMethod = "POST"

		String clientId = TestUtil.CLIENT_ID
		String clientSecret = TestUtil.CLIENT_SECRET
		auth_con.outputStream << "client_id=${clientId}&client_secret=${clientSecret}&grant_type=password&username=${username}&password=${password}"

		def body
		if (auth_con.responseCode != HttpURLConnection.HTTP_OK ||
				!(body = auth_con.content.text) || body == "") {
			println "\n\nOAuth request failed!, falling back to basic auth"
			return null
		}
		def result = new JsonSlurper().parseText(body)
		return result.access_token
	}


	static cookieString(con, cookieString) {
		con.setRequestProperty("Cookie", cookieString)
	}

	static acceptXML(con) {
		con.setRequestProperty("Accept", "application/xml");
	}

	static acceptJSON(con) {
		con.setRequestProperty("Accept", "application/json");
	}
}
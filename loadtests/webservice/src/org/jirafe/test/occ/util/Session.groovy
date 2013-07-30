package org.jirafe.test.occ.util

import groovy.json.JsonSlurper

class Session {
	def access_token
	def cookieNoPath
	def responseCode
	def responseJson
	def username, password
	
	Session() {
		// get client access token
		access_token = TestUtil.oAuth_getClientAccessToken()
	}

	Session(username, password) {
		
		this.username = username
		this.password = password
		
		// get client access token
		access_token = TestUtil.oAuth_getUserAccessToken(username, password)
	
	}

	def call(method, url, args) {
		
		def qargs = args
		if (access_token) {
			qargs = args + [access_token:access_token]
		}
		
		def queryString = qargs.collect { k,v -> URLEncoder.encode(k,'UTF-8') + "=" + URLEncoder.encode(v,'UTF-8') }.join('&')

		if (method != 'POST' && queryString) {
			url += '?' + queryString
		}
		
		def con = TestUtil.getSecureConnection(url, method)
		TestUtil.acceptJSON(con)

		if (cookieNoPath) {
			TestUtil.cookieString(con, cookieNoPath)
		}

		if (!access_token && username) {
			TestUtil.basicAuth(con, "${username}", "${password}")
		}
		
		// send access token along with other data
		if (method == 'POST') {
			con.outputStream << queryString
		}
		
		if (!cookieNoPath) {
			// use cookie to associate calls with the current session
			def cookie = con.getHeaderField('Set-Cookie')
			assert cookie : 'No cookie present!'
			cookieNoPath = cookie.split(';')[0]
		}

		def body = con.inputStream.text
		responseJson = null
		responseCode = con.responseCode
		if (body) {
			try {
				responseJson = new JsonSlurper().parseText(body)
			} catch (e) {
				println "Expected JSON, got: ${body}"
			}
		}

		return responseCode
	}

	def get(url, args=[:]) {
		return call('GET', url, args)
	}
	
	def post(url, args=[:]) {
		return call('POST', url, args)
	}
	
	def put(url, args=[:]) {
		return call('PUT', url, args)
	}
}

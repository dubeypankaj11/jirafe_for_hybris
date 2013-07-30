package org.jirafe.test.occ

import org.jirafe.test.occ.util.Session


/**
 * This script will create new customers
 * Change the loop number to set number of customers to be created
 * @author hapham1
 *
 */
class RegisterUser extends GroovyTestCase {
	
	void testMain() {
		registerCustomer()
	}
	
	static registerCustomer(session = new Session(), login=System.currentTimeMillis() + '@hybris.com', password='test', firstName='Jirafe', lastName='Test', titleCode='mr') {
		//new customer created for each loop
		session.post('/customers', [
			login:		login,
			password:	password,
			firstName:	firstName,
			lastName:	lastName,
			titleCode:	titleCode
		])
		assert session.responseCode == HttpURLConnection.HTTP_CREATED
	}
	
	static getAddress(session, create=true) {
		session.get('/customers/current/addresses')
		assert session.responseCode == HttpURLConnection.HTTP_OK
		if (session.responseJson.addresses?.size() > 0) {
			return session.responseJson.addresses[0].id
		}
		assert create == true
		session.post('/customers/current/addresses', [
			titleCode:		'mr',
			firstName:		'Jirafe',
			lastName:		'Test',
			line1:			'test1',
			line2:			'test2',
			postcode:		'12345',
			town:			'somecity',
			countryIsoCode:	'US'
		])
		return getAddress(session, false)
	}
	
	static setCartPaymentInfo(session) {
		// get customer's existing payment info
		session.get('/customers/current/paymentinfos')
		assert session.responseCode == HttpURLConnection.HTTP_OK
		if (session.responseJson.paymentInfos?.size() > 0) {
			def paymentInfo = session.responseJson.paymentInfos[0].id
			session.put("/cart/paymentinfo/${paymentInfo}")
			return
		}
		
		// No existing info, create new
		session.post('/cart/paymentinfo', [
			'accountHolderName':			'Jirafe Test',
			'cardNumber':					'4111111111111111',
			'cardType':						'visa',
			'expiryMonth':					'01',
			'expiryYear':					'2015',
			'defaultPaymentInfo':			'true',
			'saved':						'true',
			'billingAddress.titleCode':		'mr',
			'billingAddress.firstName':		'Jirafe',
			'billingAddress.lastName':		'Test',
			'billingAddress.line1':			'test1',
			'billingAddress.line2':			'test2',
			'billingAddress.postcode':		'12345',
			'billingAddress.town':			'somecity',
			'billingAddress.countryIsoCode':'US'
		])
	}

}

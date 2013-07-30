package org.jirafe.test.occ

import org.jirafe.test.occ.util.Session


/**
 * This script will place orders for user jirafe@fake.test.com.
 * User account should be created up front.
 * Change the loop number to set number of order to be created
 * @author hapham1
 *
 */
class PlaceOrder extends GroovyTestCase {
	static getSession() {
		return new Session('jirafe@fake.test.com', '1234567')
	}
	void testMain() {
		def session = getSession()
		AddProductToCart.addProductToCart(session)
		placeOrder(session)
	}
	static placeOrder(session = getSession()) {
		// get customer's address and payment info
		def address = RegisterUser.getAddress(session)

		// set a delivery address for this cart
		session.put("/cart/address/delivery/${address}")
		assert session.responseCode == HttpURLConnection.HTTP_OK

		// set a delivery mode for this cart
		session.put('/cart/deliverymodes/standard-gross')
		assert session.responseCode == HttpURLConnection.HTTP_OK

		// create a paymentinfo for this cart
		RegisterUser.setCartPaymentInfo(session)
		assert session.responseCode == HttpURLConnection.HTTP_OK

		// authorize cart
		session.post('/cart/authorize', [securityCode:'123'])
		assert session.responseCode == HttpURLConnection.HTTP_ACCEPTED

		// place order
		session.post('/cart/placeorder')
		assert session.responseCode == HttpURLConnection.HTTP_OK
	}
}

/**
 *
 */
package org.jirafe.test.occ

import org.jirafe.test.occ.util.Session

/**
 * This script will add product code 3429337 in the electronic store
 * to an anonymous cart. Change the loop number to set number of carts to be created
 * @author hapham1
 *
 */
class AddProductToCart extends GroovyTestCase {
	void testMain() {
		addProductToCart()
	}
	static addProductToCart(session=new Session(), code='3429337', qty='1') {
		// add product to cart
		session.post('/cart/entry', [code:code, qty:qty])
		assert session.responseCode == HttpURLConnection.HTTP_OK
	}
}

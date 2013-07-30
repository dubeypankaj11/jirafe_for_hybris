import org.jirafe.test.occ.AddProductToCart;
import org.jirafe.test.occ.PlaceOrder;
import org.jirafe.test.occ.util.Session

// Run this with: CLASSPATH=".:../classes" groovy Main.groovy

class Main extends GroovyTestCase {

	void testMain() {
		def session = PlaceOrder.getSession()
		for (code in ['3429337', '1934793']) {
			AddProductToCart.addProductToCart(session, code, '3')
		}
		PlaceOrder.placeOrder(session)
	}

}

from jirafe_test import JirafeTest
import unittest

class AbandonACart(JirafeTest):

    def setUp(self):
        super(AbandonACart, self).setUp()
        self.products = [1934793, 1641905, 898503]

    def test_abandon_a_cart(self):
        self.set_referer('http://www.gooogle.com')
        for product in self.products:
            self.add_product_to_cart(product)
        print 'Cart loaded with {} items and abandoned.'.format(self.cart_item_count())

if __name__ == "__main__":
    unittest.main()

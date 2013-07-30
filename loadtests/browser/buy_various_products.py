from jirafe_test import JirafeTest
import unittest

class BuyVariousProducts(JirafeTest):

    def setUp(self):
        super(BuyVariousProducts, self).setUp()
        self.products = [1934793, 1641905, 898503]

    def test_buy_various_products(self):
        for product in self.products:
            self.add_product_to_cart(product)
        self.check_out()

if __name__ == "__main__":
    unittest.main()

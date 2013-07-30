from jirafe_test import JirafeTest
import unittest

class ViewSomeProducts(JirafeTest):

    def setUp(self):
        super(ViewSomeProducts, self).setUp()
        self.products = [1934793, 1641905, 898503]

    def test_view_some_products(self):
        for product in self.products:
            self.view_a_product(product)

if __name__ == "__main__":
    unittest.main()

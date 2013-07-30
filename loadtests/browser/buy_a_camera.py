from jirafe_test import JirafeTest
import unittest

class BuyACamera(JirafeTest):
    def setUp(self):
        super(BuyACamera, self).setUp()
        self.how_many = 1

    def test_buy_a_camera(self):
        print 'About to log in...'
        self.login()
        print '... logged in.'
        for i in xrange(self.how_many):
            print 'Buying a camera...'
            self.buy_a_product(1934793)
            print '... bought a camera.'

    def buy_a_product(self, product):
        self.add_product_to_cart(product)
        self.check_out()

if __name__ == "__main__":
    unittest.main()

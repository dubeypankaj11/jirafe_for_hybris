from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
import unittest, time

class JirafeTest(unittest.TestCase):
    def setUp(self):
        self.base_url = "http://staging-hybris501-01.int.jirafe.net:9001"
        self.timeout = 30
        # Default username/password - NOT FOR USER REGISTRATION TESTS
        self.username = "jirafe@fake.test.com"
        self.password = "1234567"

        self.driver = webdriver.Firefox()
        self.driver.implicitly_wait(self.timeout)
        self.verificationErrors = []
        self.accept_next_alert = True
    
    def login(self):
        print 'Top of login'
        driver = self.driver
        print 'getting login page...'
        driver.get(self.base_url + "/yacceleratorstorefront/login?site=electronics")
        print 'Setting login fields...'
        driver.find_element_by_id("j_username").clear()
        driver.find_element_by_id("j_username").send_keys(self.username)
        driver.find_element_by_id("j_password").clear()
        driver.find_element_by_id("j_password").send_keys(self.password)
        print 'Clicking login button...'
        driver.find_element_by_id("loginForm").find_element_by_class_name('form').click()
        for i in xrange(self.timeout):
            print 'Waiting for logged in message...'
            if self.is_element_present(By.CSS_SELECTOR, "li.logged_in"):
                break
            time.sleep(1)
        else:
            self.fail("login time out")

    def view_a_product(self, product):
        driver = self.driver

        driver.get(self.base_url + "/yacceleratorstorefront/p/{}?site=electronics".format(product))
        if not driver.current_url.find('/p/{}'.format(product)) >= 0:
            self.fail('Failed to visit product: {}, got {}'.format(product, driver.current_url))

    def cart_item_count(self):
        driver = self.driver

        try:
            text = driver.find_element_by_id("minicart_data").find_element_by_tag_name('dt').text
            item_count = int(text.split()[0])
        except:
            item_count = 0

        return item_count

    def add_product_to_cart(self, product_code=None):
        driver = self.driver
        
        if product_code:
            self.view_a_product(product_code)

        before = self.cart_item_count()
        print 'Click on add to cart...'
        driver.find_element_by_id("addToCartForm").find_element_by_tag_name('button').click()
        for i in xrange(self.timeout):
            if self.cart_item_count() > before:
                break
            time.sleep(1)
        else:
            self.fail('timed out on add to cart')

    def check_out(self):
        driver = self.driver

        print 'Waiting for checkout sequence...'

        for i in xrange(self.timeout):
            driver.get(self.base_url + "/yacceleratorstorefront/cart/checkout?site=electronics")

            if driver.current_url.find('login/checkout') >= 0:
                self.login()

            if driver.current_url.find('checkout/multi') >= 0:
                print '... delivery address'
                if driver.current_url.endswith('-delivery-address'):
                    driver.get(self.base_url + "/yacceleratorstorefront/checkout/multi/choose-delivery-method")
                print '... delivery method'
                if driver.current_url.endswith('choose-delivery-method'):
                    driver.find_element_by_id("selectDeliveryMethodForm").find_element_by_tag_name('button').click()
                print '... payment method'
                if driver.current_url.endswith('add-payment-method'):
                    driver.find_element_by_class_name("saved-payment-list-entry").find_element_by_class_name("form").click()
                break
            if driver.current_url.find('checkout/single') >= 0:
                break
            time.sleep(1)
        else:
            self.fail('timed out waiting for checkout screen')

        print 'Order submit...'
        self.driver.implicitly_wait(1)
        security_code = driver.find_elements_by_id('SecurityCode')
        self.driver.implicitly_wait(self.timeout)
        if security_code:
            security_code[0].send_keys('123')
        driver.find_element_by_id("Terms1").click()
        driver.find_element_by_id("placeOrderForm1").find_element_by_tag_name('button').click()

        for i in xrange(self.timeout):
            if driver.current_url.find('orderConfirmation') >= 0:
                break
            time.sleep(1)
        else:
            self.fail('timed out waiting for order confirmation')

        print 'Done!'
    
    def is_element_present(self, how, what):
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
    def tearDown(self):
        self.driver.quit()
        self.assertEqual([], self.verificationErrors)

if __name__ == "__main__":
    unittest.main()

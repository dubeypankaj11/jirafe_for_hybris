from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
import unittest, time, os

class JirafeTest(unittest.TestCase):
    def setUp(self, user_number=None, browser=None):
        self.base_url = os.getenv('TEST_BASE_URL',
                                  'http://staging-hybris501-01.int.jirafe.net:9001')
        self.timeout = int(os.getenv('TEST_TIMEOUT', '30'))
        self.referer = os.getenv('TEST_REFERER', None)
        self.set_user_number(user_number or os.getenv('TEST_USER_NUMBER', 0))
        browser = browser or os.getenv('TEST_BROWSER', 'firefox')

        if browser == 'firefox':
            # Use Firefox
            self.driver = webdriver.Firefox()
        elif browser == 'phantomjs':
            # Use PhantomJS
            self.driver = webdriver.PhantomJS(
               service_args=['--ignore-ssl-errors=yes']
            )
        else:
            self.fail('Unsupported browser: ' + browser)

        self.driver.implicitly_wait(self.timeout)

    def tearDown(self):
        self.driver.quit()

    def set_user_number(self, user_number=0):
        self.username = "jirafe{:0>8}@fake.test.com".format(user_number)
        self.password = 'password'

    def set_referer(self, referer=None):
        self.referer = referer

    def view_a_page(self, page):
        driver = self.driver
        if self.referer:
            if not driver.current_url.startswith(self.base_url):
                driver.get(self.base_url)
            driver.add_cookie({
                'name': 'fakewww_referrer_spoof',
                'value': self.referer,
                'path': '/',
                'secure': False,
            })
        driver.get(self.base_url + page)

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
            if self.nowait(self.is_element_present, By.CSS_SELECTOR, "li.logged_in"):
                break
            time.sleep(1)
        else:
            self.fail("login time out")

    def view_a_product(self, product):
        driver = self.driver

        self.view_a_page("/yacceleratorstorefront/p/{}?site=electronics".format(product))
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
                if driver.current_url.endswith('choose-delivery-address'):
                    driver.get(self.base_url + "/yacceleratorstorefront/checkout/multi/choose-delivery-method")
                if driver.current_url.endswith('add-delivery-address'):
                    if self.nowait(driver.find_elements_by_class_name, 'saved-payment-list-entry'):
                        driver.get(self.base_url + "/yacceleratorstorefront/checkout/multi/choose-delivery-method")
                    else:
                        self.fill_form([
                            ['address.country', 'United States\t'],
                            ['address.title', 'm'],
                            ['address.firstName', 'John'],
                            ['address.surname', 'Doe'],
                            ['address.line1', '123 Main St.'],
                            ['address.townCity', 'Anytown'],
                            ['address.region', 'a'],
                            ['address.postcode', 12345]])
                        dft = self.nowait(driver.find_elements_by_class_name, 'add-address-left-input')
                        if dft:
                            # Hybris 4
                            dft[0].click()
                            done = driver.find_element_by_id('addressForm')
                        else:
                            # Hybris 5
                            driver.find_element_by_id('saveAddressInMyAddressBook').click()
                            done = driver.find_element_by_id('addressform_button_panel')
                        done.find_element_by_tag_name('button').click()
                print '... delivery method'
                if driver.current_url.endswith('choose-delivery-method'):
                    driver.find_element_by_id("selectDeliveryMethodForm").find_element_by_tag_name('button').click()
                print '... payment method'
                if driver.current_url.endswith('hop-mock'):
                    driver.find_element_by_id('button.succeed').click()
                if driver.current_url.endswith('add-payment-method'):
                    paylist = self.nowait(driver.find_elements_by_class_name, "saved-payment-list-entry")
                    if not paylist:
                        driver.find_element_by_id('useDeliveryAddress').click()
                        self.fill_form([
                            ['card_cardType', 'v'],
                            ['card_nameOnCard', 'John Doe'],
                            ['card_accountNumber', '4111111111111111'],
                            ['card_cvNumber', '123'],
                            ['ExpiryMonth', '0'],
                            ['ExpiryYear', '2020']])
                        driver.find_element_by_id('savePaymentInfo1').click()
                        driver.find_element_by_class_name('save_payment_details').find_element_by_tag_name('button').click()
                    else:
                        paylist[0].find_element_by_class_name("form").click()
                break
            if driver.current_url.find('checkout/single') >= 0:
                break
            time.sleep(1)
        else:
            self.fail('timed out waiting for checkout screen')

        print 'Order submit...'
        security_code = self.nowait(driver.find_elements_by_id, 'SecurityCode')
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
    
    def fill_form(self, map):
        driver = self.driver
        for field, value in map:
            elements = self.nowait(driver.find_elements_by_id, field)
            if elements:
                elements[0].send_keys(value)

    def nowait(self, func, *args):
        driver = self.driver
        driver.implicitly_wait(0)
        ret = func(*args)
        driver.implicitly_wait(self.timeout)
        return ret

if __name__ == "__main__":
    unittest.main()

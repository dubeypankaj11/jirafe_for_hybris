from jirafe_test import JirafeTest
import random, unittest

class Register(JirafeTest):

    def setUp(self):
        super(Register, self).setUp()
        self.how_many = 1

    def test_register(self):
        for i in xrange(self.how_many):
            self.register()

    def register(self):
        driver = self.driver

        number = "{:0>8}".format(random.randint(0,99999999))
        username = "jirafe{}@fake.test.com".format(number)
        password = 'password'

        driver.get(self.base_url + "/yacceleratorstorefront/login?site=electronics")

        for field, value in [
            ['register.title', 'm'],
            ['register.firstName', 'John'],
            ['register.lastName', 'Doe'],
            ['register.email', username],
            ['password', password],
            ['register.checkPwd', password]]:
            element = driver.find_element_by_id(field)
            #element.clear()
            element.send_keys(value)

        driver.find_element_by_id('registerForm').\
            find_element_by_tag_name('button').click()

        result = driver.find_element_by_id('globalMessages').\
            find_element_by_tag_name('p').text
        if result != 'Thank you for registering.':
            self.fail('Registration failed with message: ' + result)
        print 'Successfully registered {}'.format(username)

if __name__ == "__main__":
    unittest.main()

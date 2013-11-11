from jirafe_test import JirafeTest
import unittest

class Register(JirafeTest):

    def setUp(self):
        super(Register, self).setUp()
        self.start = 1
        self.how_many = 1

    def test_register(self):
        for i in xrange(self.start, self.start + self.how_many):
            self.register(i)

    def register(self, i):
        driver = self.driver

        self.set_user_number(i)

        driver.get(self.base_url + "/yacceleratorstorefront/login?site=electronics")

        self.fill_form([
            ['register.title', 'm'],
            ['register.firstName', 'John'],
            ['register.lastName', 'Doe'],
            ['register.email', self.username],
            ['password', self.password],
            ['register.checkPwd', self.password]])

        driver.find_element_by_id('registerForm').\
            find_element_by_tag_name('button').click()

        result = driver.find_element_by_id('globalMessages').\
            find_element_by_tag_name('p').text
        if result != 'Thank you for registering.':
            for err in self.nowait(driver.find_elements_by_class_name, 'form_field_error-message'):
                result += '\n' + err.text
            registerForm = driver.find_element_by_id('registerForm')
            if registerForm:
                for err in self.nowait(registerForm.find_elements_by_tag_name, 'p'):
                    result += '\n' + err.text
            self.fail('Registration failed with message: ' + result)
        print 'Successfully registered {}'.format(self.username)

if __name__ == "__main__":
    unittest.main()

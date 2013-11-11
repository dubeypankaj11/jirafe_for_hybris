# Load tests

These browser tests provide realistic simulation of a user browsing the site
with a browser. These tests will generate cookies and tracker events.

## Local Setup
```
virtualenv --no-site-packages --use-distribute  venv
source venv/bin/activate
pip install -r requirements.txt
```
There are a number of tests available. The names should be self-explanatory and
can be run from a command line using:
```
python something.py
```

## Global settings

There's a file called jirafe_test.py that contains a setUp section with some
global settings:

* base_url - edit to point at the store you're testing
* timeout - how long to wait for a response before failing
* user_number - user number to use for the test, create users with register.py
* browser - firefox or phantomjs

These can also be set via the environment, eg. TEST_BASE_URL.

Also check the setUp section of the individual tests which include such things as:

* products - a list of product ids for tests that reference multiple products
* how_many - a count of how many iterations to run

## Current tests

* abandon_a_cart - load a cart and abandon it
* buy_a_camera - load a camera into a cart and check out repeatedly
* buy_various_products - load a number of items into a cart and check out
* register - register users repeatedly
* view_some_products - visit some product pages

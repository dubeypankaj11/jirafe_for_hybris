# Load tests

We have two sets of load tests: browser and webservice.

The browser tests provide realistic simulation of a user browsing the site
with a browser. These tests will generate cookies and tracker events.

The webservice tests use Hybris's rest api to communicate with the back end
directly. These tests won't pass cookies but can be used to generate more
load per client than a browser-based test.

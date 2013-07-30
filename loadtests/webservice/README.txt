To run  test scripts:

1. Import the following Impex script to create customer account used to place orders (this is one-time setup per hybris instance) 

$passwordEncoding=md5
$defaultPassword=1234567
$setPassword=@password[translator=de.hybris.platform.impex.jalo.translators.ConvertPlaintextToEncodedUserPasswordTranslator][default='$passwordEncoding:$defaultPassword']

INSERT_UPDATE Customer;UID[unique=true];$setPassword;description;name;groups(uid)
;"jirafe@fake.test.com";;"Jirafe Test Customer";"Jirafe Test Customer";"customergroup"


2. Modify host and port in file src/org/jirafe/test/occ/util/TestUtil.groovy if needed

3. cd to folder jirafetestwithocc-oauth/src
	
4. Create folder to store compiled Java classes
	mkdir ../classes

5. Compile Java files (
	javac -d ../classes ./org/jirafe/test/occ/util/*.java


*Note*: steps 1 to 5 only need to be done 1 time

6. Modify number of loops (number of data records -orders, carts, customers- created) in each test file if you wish

7. Run Groovy tests (still in folder jirafetestwithocc-oauth/src/org/jirafe/test/occ)
	CLASSPATH="../classes" groovy ./org/jirafe/test/occ/<Groovy file>
	
	

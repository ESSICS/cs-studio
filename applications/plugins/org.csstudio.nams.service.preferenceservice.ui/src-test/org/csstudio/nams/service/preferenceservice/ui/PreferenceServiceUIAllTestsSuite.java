package org.csstudio.nams.service.preferenceservice.ui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class PreferenceServiceUIAllTestsSuite extends TestCase {

	public static Test suite() throws Throwable {
		
		TestSuite suite = new TestSuite("PreferenceServiceAllTestsSuite");
		//$JUnit-BEGIN$

		suite.addTestSuite(PreferenceServiceUIAllTestsSuite.class);


		//$JUnit-END$
		return suite;
	}

	@org.junit.Test
	public void testAssertionsAktiviert()
	{
		try {
			assert false : "Ok, Assertions sind aktiviert!";
			fail("Nein, Assertions sind nicht aktiviert");
		} catch(AssertionError ae) {
			assertEquals("Ok, Assertions sind aktiviert!", ae.getMessage());
		}
	}
}

package edu.berkeley.cspace.test;

import org.testng.annotations.Test;

public class CitationIT extends CollectionSpaceIT {

	/**
	 * Tests that all editable fields are successfully saved.
	 * <ul>
	 * <li>All fields should retain their value after saving</li>
	 * </ul>
	 * @throws SaveFailedException 
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testAllFields() throws SaveFailedException {
		testAllFields(Page.CITATION);
	}
}

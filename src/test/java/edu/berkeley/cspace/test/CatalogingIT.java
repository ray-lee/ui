package edu.berkeley.cspace.test;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CatalogingIT extends CollectionSpaceIT {

	/**
	 * Tests that all editable fields are successfully saved.
	 * <ul>
	 * <li>All fields should retain their value after saving</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testAllFields() {
		driver.navigateTo(Page.CATALOGING);

		Map<String, Object> values = driver.fillAllFields();
		MapUtils.debugPrint(System.out, "values", values);	
	}
}

package edu.berkeley.cspace.test;

import java.util.Map;

import org.testng.annotations.Test;

public class LocationIT extends CollectionSpaceIT {

	/**
	 * Tests that all editable fields are successfully saved.
	 * <ul>
	 * <li>All fields should retain their value after saving</li>
	 * </ul>
	 * @throws SaveFailedException 
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testAllFields() throws SaveFailedException {
		testAllFields(Page.LOCATION);
	}
	
	@Override
	public Map<String, Object> getExpectedAfterSaveFieldValues(Map<String, Object> beforeSaveValues) {
		Map<String, Object> values = super.getExpectedAfterSaveFieldValues(beforeSaveValues);

		values.put("csc-location-conditionNoteDate", getExpectedCalendarDate((String) values.get("csc-location-conditionNoteDate")));

		return values;
	}
}

package edu.berkeley.cspace.test;

import java.util.Map;

import org.testng.annotations.Test;

public class MediaIT extends CollectionSpaceIT {

	/**
	 * Tests that all editable fields are successfully saved,
	 * excluding the Upload Media and Link to External Media
	 * fields.
	 * <ul>
	 * <li>All fields should retain their value after saving</li>
	 * </ul>
	 * @throws SaveFailedException 
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testAllFields() throws SaveFailedException {
		testAllFields(Page.MEDIA);
	}
	
	@Override
	public Map<String, Object> getExpectedAfterSaveFieldValues(Map<String, Object> beforeSaveValues) {
		Map<String, Object> values = super.getExpectedAfterSaveFieldValues(beforeSaveValues);

		values.put("csc-dimension-valueDate", getExpectedCalendarDate((String) values.get("csc-dimension-valueDate")));

		return values;
	}
	
	public void testUploadFile() throws SaveFailedException {
		// TODO
	}
	
	public void testLinkToExternalFile() throws SaveFailedException {
		// TODO
	}
}

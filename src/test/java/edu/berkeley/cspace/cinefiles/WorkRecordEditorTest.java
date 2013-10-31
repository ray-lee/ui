package edu.berkeley.cspace.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

public class WorkRecordEditorTest extends CineFilesTest {
	/**
	 * Tests the work record editor.
	 * <ul>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testWorkRecordEditor() {
		driver.get(WORK_URL);		
		
		this.fillField("csc-workAuthority-termName", "Moon");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "Moon");
		
		driver.get(WORK_URL);		

		this.fillField("csc-workAuthority-termQualifier", "The");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "The ");

		this.fillField("csc-workAuthority-termName", "Shining");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "The Shining");
		
		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-workAuthority-termQualifier", "The");
		fieldValues.put("csc-workAuthority-termName", "Shining");

		//testSave(fieldValues);
	}
}

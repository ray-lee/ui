package edu.berkeley.cspace.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class CreateWorkIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreateWorkIT.class);

	public static final String WORK_URL = BASE_URL + "/work.html?vocab=work";

	/**
	 * Tests creating a work record.
	 * <ul>
	 * <li>Display name should be computed correctly from the article and title</li>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test (dependsOnMethods = { "testLogin" })
	public void testCreateWork() {
		navigateTo(WORK_URL);
		
		// If the article is undefined, the display name should just be the title.
		this.fillField("csc-workAuthority-termName", "Moon");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "Moon");
		
		navigateTo(WORK_URL);

		// If the title is undefined, the display name should just be the article (plus whitespace).
		// For most articles, there should be a space between the article and the title.
		
		this.fillField("csc-workAuthority-termQualifier", "The");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "The ");

		this.fillField("csc-workAuthority-termName", "Shining");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "The Shining");

		// For articles ending in apostrophe, there should be no space.
		
		this.fillField("csc-workAuthority-termQualifier", "L'");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "L'Shining");
		
		this.clearField("csc-workAuthority-termName");
		this.fillField("csc-workAuthority-termName", "eau froide");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "L'eau froide");
		
		// For articles ending in dash, there should be no space.
		
		this.fillField("csc-workAuthority-termQualifier", "El-");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "El-eau froide");
		
		this.clearField("csc-workAuthority-termName");
		this.fillField("csc-workAuthority-termName", "Gazaeria");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "El-Gazaeria");
		
		navigateTo(WORK_URL);

		// If the title is empty, the display name should just be the article (plus whitespace).
		
		this.fillField("csc-workAuthority-termQualifier", "An");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "An ");

		this.fillField("csc-workAuthority-termName", "");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "An ");

		this.fillField("csc-workAuthority-termName", "Education");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "An Education");

		// If the article is empty, the display name should just be the title.
		
		this.fillSelectFieldByValue("csc-workAuthority-termQualifier", "");
		this.testFieldHasValue("csc-workAuthority-termDisplayName", "Education");

		navigateTo(WORK_URL);

		// Extension fields should save.

		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-workAuthority-termQualifier", "The");
		fieldValues.put("csc-workAuthority-termName", "Shining");
		fieldValues.put("csc-work-country", "United States");
		fieldValues.put("csc-work-language", "English");
		fieldValues.put("csc-work-genre", "Horror");
		fieldValues.put("csc-work-theme", "Theme 5");
		
		testSave(fieldValues);
	}
}

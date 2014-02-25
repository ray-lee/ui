package edu.berkeley.cspace.test.cinefiles;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class CreateCatalogingIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreateCatalogingIT.class);

	public static final String CATALOGING_URL = BASE_URL + "/cataloging.html";
	public static final List<String> checkboxFields = Arrays.asList(
			"hasCastCr", "hasTechCr", "hasBoxInfo", "hasFilmog", "hasBiblio", "hasDistCo", "hasProdCo", "hasCostInfo", "hasIllust");

	/**
	 * Tests creating a cataloging record.
	 * <ul>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCreateCataloging() {
		navigateTo(CATALOGING_URL);
		
		String identificationNumber = chooseNextNumber("csc-object-identification-object-number", "Accession");
		clearField("csc-object-identification-object-number");
		
		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-object-identification-object-number", identificationNumber);
		fieldValues.put("csc-collection-object-docTitle", "Review");
		fieldValues.put("csc-object-identification-number-objects", "7");
		fieldValues.put("csc-collection-object-pageInfo", "pages 11-17");
		fieldValues.put("csc-collection-object-docType", "Option 1");
		fieldValues.put("csc-collection-object-docSubject", "Subject 1");
		fieldValues.put("csc-collection-object-nameSubject", "John Doe");
		fieldValues.put("csc-collection-object-filmSubject", "Moon");
		fieldValues.put("csc-collection-object-docLanguage", "English");
		fieldValues.put("csc-collection-object-source", "Jane Doe");
		fieldValues.put("csc-collection-object-accessCode", "World");
		
		for (String checkboxField : checkboxFields) {
			fieldValues.put("csc-collection-object-" + checkboxField, "true");
		}

		testSave(fieldValues);
	}
	
	/**
	 * Tests creating cataloging records, each with a single checkbox checked.
	 * This ensures that each checkbox is wired correctly.
	 */
	@Test(dependsOnMethods = { "testCreateCataloging" })
	public void testCheckboxes() {		
		for (String checkedField : checkboxFields) {
			navigateTo(CATALOGING_URL);
			
			String identificationNumber = chooseNextNumber("csc-object-identification-object-number", "Accession");
			clearField("csc-object-identification-object-number");
			
			Map<String, String> fieldValues = new LinkedHashMap<String, String>();
			fieldValues.put("csc-object-identification-object-number", identificationNumber);
			fieldValues.put("csc-collection-object-docTitle", "Review");
			fieldValues.put("csc-object-identification-number-objects", "7");
	
			for (String field : checkboxFields) {
				fieldValues.put("csc-collection-object-" + field, field.equals(checkedField) ? "true" : "false");
			}
			
			testSave(fieldValues);
		}
	}
}

package edu.berkeley.cspace.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class CreateCatalogingIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreateCatalogingIT.class);

	public static final String CATALOGING_URL = BASE_URL + "/cataloging.html";

	/**
	 * Tests creating a cataloging record.
	 *<ul>
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
		fieldValues.put("csc-collection-object-pageInfo", "pages 11-17");
		fieldValues.put("csc-collection-object-docType", "Option 1");
		fieldValues.put("csc-collection-object-docSubject", "Subject 1");
		fieldValues.put("csc-collection-object-nameSubject", "John Doe");
		fieldValues.put("csc-collection-object-filmSubject", "Moon");
		fieldValues.put("csc-collection-object-docLanguage", "English");
		fieldValues.put("csc-collection-object-source", "John Doe");
		fieldValues.put("csc-collection-object-accessCode", "World");
		fieldValues.put("csc-collection-object-hasCastCr", "true");

		//testSave(fieldValues);
	}
}

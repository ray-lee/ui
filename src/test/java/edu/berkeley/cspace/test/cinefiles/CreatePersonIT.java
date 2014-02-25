package edu.berkeley.cspace.test.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CreatePersonIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreatePersonIT.class);

	public static final String PERSON_URL = BASE_URL + "/person.html?vocab=person";

	/**
	 * Tests creating a person record.
	 * <ul>
	 * <li>The birthPlace field should be tied to the country vocabulary</li>
	 * <li>The contact addressCountry term list should load (BAMPFA-37)</li>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test (dependsOnMethods = { "testLogin" })
	public void testCreatePerson() {
		navigateTo(PERSON_URL);
		
		Assert.assertTrue(isTiedToCountryTermList("csc-personAuthority-birthPlace"), "birthPlace should be tied to country term list");
		testContactAddressCountryLoads();
		
		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-personAuthority-termDisplayName", "Test Person");
		fieldValues.put("csc-person-birthCity", "City");
		fieldValues.put("csc-person-birthState", "State");
		fieldValues.put("csc-personAuthority-birthPlace", "Cote d'Ivoire");
		fieldValues.put("csc-person-accessCode", "PFA Staff Only");
		fieldValues.put("csc-contact-addressCountry", "United States");

		testSave(fieldValues);
	}
}

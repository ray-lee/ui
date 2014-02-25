package edu.berkeley.cspace.test.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CreateOrganizationIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreateOrganizationIT.class);

	public static final String ORGANIZATION_URL = BASE_URL + "/organization.html?vocab=organization";

	/**
	 * Tests creating an organization record.
	 * <ul>
	 * <li>The foundingPlace field should be tied to the country vocabulary</li>
	 * <li>The contact addressCountry term list should load (BAMPFA-37)</li>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCreateOrganization() {
		navigateTo(ORGANIZATION_URL);
		
		Assert.assertTrue(isTiedToCountryTermList("csc-orgAuthority-foundingPlace"), "foundingPlace should be tied to country term list");
		testContactAddressCountryLoads();
		
		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-orgAuthority-termDisplayName", "Test Org");
		fieldValues.put("csc-organization-foundingCity", "City");
		fieldValues.put("csc-organization-foundingState", "State");
		fieldValues.put("csc-orgAuthority-foundingPlace", "Australia");
		fieldValues.put("csc-organization-accessCode", "Campus (UCB)");
		fieldValues.put("csc-organization-member", "Michael Member");
		fieldValues.put("csc-organization-memberNote", "Member note");
		fieldValues.put("csc-organization-memberAuthority", "Citation 1");
		fieldValues.put("csc-contact-addressCountry", "England");

		testSave(fieldValues);
	}
}

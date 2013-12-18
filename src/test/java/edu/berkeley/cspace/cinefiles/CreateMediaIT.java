package edu.berkeley.cspace.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class CreateMediaIT extends CineFilesIT {
	public static final Logger logger = Logger.getLogger(CreateMediaIT.class);

	public static final String MEDIA_URL = BASE_URL + "/media.html";

	/**
	 * Tests creating a media record.
	 * <ul>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCreateMedia() {
		navigateTo(MEDIA_URL);
				
		Map<String, String> fieldValues = new LinkedHashMap<String, String>();
		fieldValues.put("csc-media-page", "1");

		testSave(fieldValues);
	}
}

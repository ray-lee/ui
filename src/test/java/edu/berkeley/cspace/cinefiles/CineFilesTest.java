package edu.berkeley.cspace.cinefiles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class CineFilesTest {
	public static final Logger logger = Logger.getLogger(CineFilesTest.class);
	
	public static final String HOST = "cspace-vm";
	public static final String TENANT_NAME = "cinefiles";
	public static final String USERNAME = "admin@cinefiles.cspace.berkeley.edu";
	public static final String PASSWORD = "Administrator";

	public static final String BASE_URL = "http://" + HOST + ":8180/collectionspace/ui/" + TENANT_NAME + "/html";
	public static final String LOGIN_URL = BASE_URL + "/index.html";
	public static final String PERSON_URL = BASE_URL + "/person.html?vocab=person";
	public static final String ORGANIZATION_URL = BASE_URL + "/organization.html?vocab=organization";
	public static final String WORK_URL = BASE_URL + "/work.html?vocab=work";
	
	public static final long TIMEOUT = 5;
	public static final long SAVE_TIMEOUT = 10;
	
	protected WebDriver driver = new FirefoxDriver();
	
	@BeforeClass
	public void setUp() {	
		driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
	}

	@AfterClass
	public void finish() {	
		driver.close();
	}

	/**
	 * Tests logging in to the tenant.
	 * <ul>
	 * <li>Login should succeed</li>
	 * <li>The landing page should be the Find and Edit page</li>
	 * </ul>
	 */
//	@Test
	public void testLogin() {
		driver.get(LOGIN_URL);

		try {
			driver.findElement(By.className("csc-login-userId")).sendKeys(USERNAME);
			driver.findElement(By.className("csc-login-password")).sendKeys(PASSWORD);
			driver.findElement(By.className("csc-login-button")).click();
		}
		catch(NoSuchElementException e) {
			Assert.fail("Login fields not found - CollectionSpace may not be running");
			return;
		}
		
		WebElement logoutElement = null;
		
		try {
			logoutElement = driver.findElement(By.className("csc-header-logout-form"));
		}
		catch(NoSuchElementException e) { }
		
		if (logoutElement == null) {
			WebElement errorElement = null;
			
			try {
				errorElement = driver.findElement(By.className("cs-message-error"));
			}
			catch(NoSuchElementException e) { }
			
			if (errorElement == null) {
				Assert.fail("Login failed with no error message");
			}
			else {
				WebElement messageElement = errorElement.findElement(By.id("message"));
				String errorMessage = messageElement.getText();
				
				Assert.fail("Login failed with error: " + errorMessage);
			}
		}
		else {
			Assert.assertEquals(driver.getTitle(), "CollectionSpace - Find and Edit", "Incorrect landing page");		
		}
	}

	/**
	 * Tests the organization record editor.
	 * <ul>
	 * <li>The foundingPlace field should be tied to the country vocabulary</li>
	 * <li>The contact addressCountry term list should load (BAMPFA-37)</li>
	 * <li>All extension fields should save</li>
	 * </ul>
	 */
	//@Test(dependsOnMethods = { "testLogin" })
	public void testOrganizationRecordEditor() {
		driver.get(ORGANIZATION_URL);
		
		Assert.assertTrue(isTiedToCountryTermList("csc-orgAuthority-foundingPlace"), "foundingPlace is not tied to country term list");
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
	
	protected void testSave(Map<String, String> fieldValues) {
		fillForm(fieldValues);
		testSaveForm();
		
		// Pause to let all the term lists load
		pause(2000);
		closeMessageBar();
		
		testFormContainsValues(fieldValues);
	}
	
	protected void testSaveForm() {
		final String saveButtonClassName = "csc-save";
		WebElement saveButtonElement = null;

		try {
			saveButtonElement = driver.findElement(By.className(saveButtonClassName));
		}
		catch(NoSuchElementException e) {
			logger.warn("Failed to find save button with class: " + saveButtonClassName);
		}
		
		if (saveButtonElement != null) {
			saveButtonElement.click();
		}

		driver.manage().timeouts().implicitlyWait(SAVE_TIMEOUT, TimeUnit.SECONDS);

		try {
			WebElement messageElement = driver.findElement(By.className("csc-messageBar-message"));
			String messageText = messageElement.getText();
			Assert.assertTrue(messageText.contains("success"), "Save failed with error: " + messageText);
		}
		catch(NoSuchElementException e) {
			Assert.fail("Save did not complete within the timeout period");
		}
		finally {
			driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
		}
	}
	
	protected void testFormContainsValues(Map<String, String> fieldValues) {
		for (String className : fieldValues.keySet()) {
			testFieldHasValue(className, fieldValues.get(className));
		}
	}
	
	protected void testFieldHasValue(String className, String value) {
		WebElement element = null;
		
		try {
			element = driver.findElement(By.className(className));
		}
		catch(NoSuchElementException e) {
			Assert.fail("No field found for class: " + className);
		}
		
		if (element != null) {		
			if (isCheckbox(element)) {
				
			}
			else if (isSelect(element)) {
				testSelectFieldHasValue(className, element, value);
			}
			else if (isAutocomplete(element)) {
				testAutocompleteFieldHasValue(className, element, value);
			}
			else if (isText(element)) {
				testTextFieldHasValue(className, element, value);
			}
			else {
				logger.warn("Unknown field type for class: " + className);
			}
		}
	}
	
	protected void testSelectFieldHasValue(String className, WebElement element, String expectedValue) {
		WebElement selectedOptionElement = null;
		
		for (WebElement candidateOptionElement : element.findElements(By.tagName("option"))) {
			if (candidateOptionElement.isSelected()) {
				selectedOptionElement = candidateOptionElement;
				break;
			}
		}
		
		if (selectedOptionElement == null) {
			Assert.fail("No value selected for field " + className);
		}
		else {
			String currentValue = selectedOptionElement.getText();

			Assert.assertEquals(currentValue, expectedValue, "Incorrect value for field " + className);
		}
	}

	protected void testAutocompleteFieldHasValue(String className, WebElement element, String expectedValue) {
		WebElement autocompleteInputElement = findSiblingAutocompleteInputElement(element);
		
		if (autocompleteInputElement != null) {
			String currentValue = autocompleteInputElement.getAttribute("value");

			Assert.assertEquals(currentValue, expectedValue, "Incorrect value for field " + className);
		}
	}

	protected void testTextFieldHasValue(String className, WebElement element, String expectedValue) {
		String tagName = element.getTagName();
		String currentValue = "";
		
		if (tagName.equalsIgnoreCase("textarea")) {
			currentValue = element.getText();
		}
		else {
			currentValue = element.getAttribute("value");
		}

		Assert.assertEquals(currentValue, expectedValue, "Incorrect value for field " + className);
	}
	
	protected void fillForm(Map<String, String> fieldValues) {
		for (String className : fieldValues.keySet()) {
			fillField(className, fieldValues.get(className));
		}
	}
	
	protected void fillField(String className, String value) {
		WebElement element = null;
		
		try {
			element = driver.findElement(By.className(className));
		}
		catch(NoSuchElementException e) {
			logger.warn("No field found for class: " + className);
		}
		
		if (element != null) {		
			if (isCheckbox(element)) {
				
			}
			else if (isSelect(element)) {
				fillSelectField(element, value);
			}
			else if (isAutocomplete(element)) {
				fillAutocompleteField(element, value);
			}
			else if (isText(element)) {
				fillTextField(element, value);
			}
			else {
				logger.warn("Unknown field type for class: " + className);
			}
		}
	}
	
	protected boolean isCheckbox(WebElement element) {
		return false;
	}
	
	protected boolean isSelect(WebElement element) {
		String tagName = element.getTagName();

		return tagName.equalsIgnoreCase("select");
	}
	
	protected boolean isAutocomplete(WebElement element) {
		boolean isAutocomplete = false;
		
		if (isText(element)) {
			WebElement autocompleteInputElement = findSiblingAutocompleteInputElement(element);
			
			if (autocompleteInputElement != null) {
				isAutocomplete = true;
			}
		}
		
		return isAutocomplete;
	}
	
	protected WebElement findSiblingAutocompleteInputElement(WebElement element) {
		WebElement autocompleteInputElement = null;
		
		try {
			driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
			autocompleteInputElement = element.findElement(By.xpath("following-sibling::input[@class=\"cs-autocomplete-input\"]"));
			driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
		}
		catch(NoSuchElementException e) { }
		
		return autocompleteInputElement;
	}
	
	protected boolean isText(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equalsIgnoreCase("textarea") || (tagName.equalsIgnoreCase("input") && element.getAttribute("type").equalsIgnoreCase("text")));
	}
	
	protected void fillSelectField(WebElement element, String value) {
		WebElement optionElement = null;
		
		for (WebElement candidateOptionElement : element.findElements(By.tagName("option"))) {
			if (candidateOptionElement.getText().equals(value)) {
				optionElement = candidateOptionElement;
				break;
			}
		}
		
		if (optionElement == null) {
			logger.warn("No option found in select for value: " + value);
		}
		else {
			element.click();
			optionElement.click();
			element.sendKeys("\n");
		}
	}
	
	protected void fillAutocompleteField(WebElement element, String value) {
		WebElement autocompleteInputElement = findSiblingAutocompleteInputElement(element);
		
		if (autocompleteInputElement != null) {
			autocompleteInputElement.click();
			autocompleteInputElement.sendKeys(value);
			
			WebElement popupElement = driver.findElement(By.className("cs-autocomplete-popup"));
			WebElement matchesElement = popupElement.findElement(By.className("csc-autocomplete-Matches"));
			WebElement matchSpanElement = null;
			
			for (WebElement candidateMatchElement : matchesElement.findElements(By.tagName("li"))) {
				WebElement candidateMatchSpanElement = candidateMatchElement.findElement(By.tagName("span"));
				
				if (candidateMatchSpanElement.getText().equals(value)) {
					matchSpanElement = candidateMatchSpanElement;
					break;
				}
			}
			
			if (matchSpanElement != null) {
				matchSpanElement.click();
			}
			else {
				logger.debug("Adding term: " + value);

				WebElement addToPanelElement = popupElement.findElement(By.className("csc-autocomplete-addToPanel"));
				WebElement firstAuthorityItem = addToPanelElement.findElement(By.tagName("li"));
				
				firstAuthorityItem.click();
			}
		}
	}
	
	protected void fillTextField(WebElement element, String value) {
		element.click();
		element.sendKeys(value);
		element.sendKeys("\n");
	}

	protected void testContactAddressCountryLoads() {
		Assert.assertTrue(isTiedToCountryTermList("csc-contact-addressCountry"), "Contact addressCountry term list did not load");
	}
	
	protected boolean isTiedToCountryTermList(String className) {
		try {
			driver.findElement(By.cssSelector("select." + className + " option[value^=\"urn:cspace:cinefiles.cspace.berkeley.edu:vocabularies:name(country)\"]"));
		}
		catch(NoSuchElementException e) {
			return false;
		}
		
		return true;
	}
	
	protected void closeMessageBar() {
		try {		
			driver.findElement(By.className("csc-messageBar-cancel")).click();
		}
		catch(NoSuchElementException e) {
			logger.warn("Failed to find message bar cancel button");
		}
	}
	
	protected void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {}
	}
}

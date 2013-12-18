package edu.berkeley.cspace.cinefiles;

import java.util.List;
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


public class CineFilesIT {
	public static final Logger logger = Logger.getLogger(CineFilesIT.class);
	
	public static final String HOST = "cspace-vm";
	public static final String TENANT_NAME = "cinefiles";
	public static final String USERNAME = "admin@cinefiles.cspace.berkeley.edu";
	public static final String PASSWORD = "Administrator";

	public static final String BASE_URL = "http://" + HOST + ":8180/collectionspace/ui/" + TENANT_NAME + "/html";
	public static final String LOGIN_URL = BASE_URL + "/index.html";
	
	public static final long DEFAULT_TIMEOUT = 5;
	public static final long SAVE_TIMEOUT = 10;
	
	public static final long PAGE_LOAD_PAUSE = 2;
	public static final long NUMBER_GENERATOR_PAUSE = 1;
	public static final long ADD_TERM_PAUSE = 1;
	
	protected WebDriver driver;
	
	@BeforeClass
	public void setUp() {
		driver = new FirefoxDriver();
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
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
	@Test
	public void testLogin() {		
		navigateTo(LOGIN_URL);

		try {
			driver.findElement(By.className("csc-login-userId")).sendKeys(USERNAME);
			driver.findElement(By.className("csc-login-password")).sendKeys(PASSWORD);
			driver.findElement(By.className("csc-login-button")).click();
		}
		catch(NoSuchElementException e) {
			Assert.fail("login fields not found");
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
				Assert.fail("login failed with no error message");
			}
			else {
				WebElement messageElement = errorElement.findElement(By.id("message"));
				String errorMessage = messageElement.getText();
				
				Assert.fail("login failed with error " + errorMessage);
			}
		}
		else {
			Assert.assertEquals(driver.getTitle(), "CollectionSpace - Find and Edit", "incorrect landing page");		
		}
	}
	
	protected void testSave(Map<String, String> fieldValues) {
		fillForm(fieldValues);
		testSaveForm();
		
		// Pause to let AJAX calls settle down
		pause(PAGE_LOAD_PAUSE);
		
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
			logger.warn("failed to find save button with class " + saveButtonClassName);
		}
		
		if (saveButtonElement != null) {
			saveButtonElement.click();
		}

		driver.manage().timeouts().implicitlyWait(SAVE_TIMEOUT, TimeUnit.SECONDS);

		try {
			WebElement messageElement = driver.findElement(By.className("csc-messageBar-message"));
			String messageText = messageElement.getText();
			Assert.assertTrue(messageText.contains("success"), "save should succeed without error (" + messageText + ")");
		}
		catch(NoSuchElementException e) {
			Assert.fail("save did not complete within " + SAVE_TIMEOUT + " seconds");
		}
		finally {
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		}
	}
	
	protected void testFormContainsValues(Map<String, String> fieldValues) {
		for (String className : fieldValues.keySet()) {
			testFieldHasValue(className, fieldValues.get(className));
		}
	}
	
	protected void testFieldHasValue(String className, String value) {
		WebElement element = null;
		
		List<WebElement> elements = driver.findElements(By.className(className));
		
		if (elements.size() > 1) {
			Assert.fail("multiple fields found for class " + className);
		}
		else if (elements.size() < 1) {
			Assert.fail("no field found for class " + className);
		}
		else {
			element = elements.get(0);
		}
		
		if (element != null) {		
			if (isCheckbox(element)) {
				testCheckboxHasValue(className, element, value);
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
				logger.warn("unknown field type for class " + className);
			}
		}
	}
	
	protected void testCheckboxHasValue(String className, WebElement element, String expectedValue) {
		String currentValue = element.getAttribute("checked");
		
		if (currentValue == null) {
			currentValue = "false";
		}
		
		Assert.assertEquals(currentValue, expectedValue, "incorrect value for field " + className);
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
			Assert.fail("no value selected for field " + className);
		}
		else {
			String currentValue = selectedOptionElement.getText();

			Assert.assertEquals(currentValue, expectedValue, "incorrect value for field " + className);
		}
	}

	protected void testAutocompleteFieldHasValue(String className, WebElement element, String expectedValue) {
		WebElement autocompleteInputElement = findSiblingAutocompleteInputElement(element);
		
		if (autocompleteInputElement != null) {
			String currentValue = autocompleteInputElement.getAttribute("value");

			Assert.assertEquals(currentValue, expectedValue, "incorrect value for field " + className);
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

		Assert.assertEquals(currentValue, expectedValue, "incorrect value for field " + className);
	}
	
	protected void fillForm(Map<String, String> fieldValues) {
		for (String className : fieldValues.keySet()) {
			fillField(className, fieldValues.get(className));
		}
	}
	
	protected void clearField(String className) {
		WebElement element = null;
		
		try {
			element = driver.findElement(By.className(className));
		}
		catch(NoSuchElementException e) {
			logger.warn("no field found for class " + className);
		}
		
		if (element != null) {
			element.clear();
		}
	}
	
	protected void fillField(String className, String value) {
		WebElement element = null;
		
		List<WebElement> elements = driver.findElements(By.className(className));
		
		if (elements.size() > 1) {
			logger.warn("multiple fields found for class " + className);
		}
		else if (elements.size() < 1) {
			logger.warn("no field found for class " + className);
		}
		else {
			element = elements.get(0);
		}
		
		if (element != null) {
			if (isCheckbox(element)) {
				fillCheckbox(element, value);
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
				logger.warn("unknown field type for class " + className);
			}
		}
	}
	
	protected boolean isCheckbox(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equalsIgnoreCase("input") && element.getAttribute("type").equalsIgnoreCase("checkbox"));
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
	
	protected boolean isNumberPattern(WebElement element) {
		boolean isNumberPattern = false;
		
		if (isText(element)) {
			WebElement chooserElement = findSiblingNumberPatternChooserElement(element);
			
			if (chooserElement != null) {
				isNumberPattern = true;
			}
		}
		
		return isNumberPattern;
	}
	
	protected WebElement findSiblingAutocompleteInputElement(WebElement element) {
		return findSiblingElementByClass(element, "cs-autocomplete-input");
	}
	
	protected WebElement findSiblingNumberPatternChooserElement(WebElement element) {
		return findSiblingElementByClass(element, "cs-numberPatternChooserContainer");
	}
	
	protected WebElement findSiblingElementByClass(WebElement element, String className) {
		WebElement foundElement = null;
		
		try {
			driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
			foundElement = element.findElement(By.xpath("following-sibling::*[@class=\"" + className + "\"]"));
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		}
		catch(NoSuchElementException e) { }
		
		return foundElement;	
	}
	
	protected boolean isText(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equalsIgnoreCase("textarea") || (tagName.equalsIgnoreCase("input") && element.getAttribute("type").equalsIgnoreCase("text")));
	}
	
	protected void fillCheckbox(WebElement element, String text) {
		String checked = element.getAttribute("checked");
		
		if (checked == null) {
			checked = "false";
		}
		
		if (!checked.equals(text)) {
			element.click();
		}
	}
	
	protected void fillSelectField(WebElement element, String text) {
		WebElement optionElement = null;
		
		for (WebElement candidateOptionElement : element.findElements(By.tagName("option"))) {
			if (candidateOptionElement.getText().equals(text)) {
				optionElement = candidateOptionElement;
				break;
			}
		}
		
		if (optionElement == null) {
			logger.warn("no option found in select for text " + text);
		}
		else {
			element.click();
			optionElement.click();
			element.sendKeys("\n");
		}
	}

	protected void fillSelectFieldByValue(String className, String value) {
		WebElement element = null;
		
		try {
			element = driver.findElement(By.className(className));
		}
		catch(NoSuchElementException e) {
			logger.warn("no field found for class " + className);
		}
		
		if (element != null) {
			fillSelectFieldByValue(element, value);
		}
	}
	
	protected void fillSelectFieldByValue(WebElement element, String value) {
		WebElement optionElement = null;
		
		for (WebElement candidateOptionElement : element.findElements(By.tagName("option"))) {
			if (candidateOptionElement.getAttribute("value").equals(value)) {
				optionElement = candidateOptionElement;
				break;
			}
		}
		
		if (optionElement == null) {
			logger.warn("no option found in select for value " + value);
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
				logger.debug("found term " + value);
				matchSpanElement.click();
			}
			else {
				logger.debug("adding term " + value);

				WebElement addToPanelElement = popupElement.findElement(By.className("csc-autocomplete-addToPanel"));
				WebElement firstAuthorityItem = addToPanelElement.findElement(By.tagName("li"));
				
				firstAuthorityItem.click();
				pause(ADD_TERM_PAUSE);
			}
		}
	}
	
	protected void fillTextField(WebElement element, String value) {
		element.click();
		element.sendKeys(value);
		element.sendKeys("\n");
	}

	protected String chooseNextNumber(String className) {
		return chooseNextNumber(className, null);
	}
	
	protected String chooseNextNumber(String className, String patternName) {
		WebElement element = null;
		String nextNumber = "";
		
		try {
			element = driver.findElement(By.className(className));
		}
		catch(NoSuchElementException e) {
			logger.warn("no field found for class " + className);
		}
		
		if (element != null) {
			if (isNumberPattern(element)) {
				chooseNextNumber(findSiblingNumberPatternChooserElement(element), patternName);
				pause(NUMBER_GENERATOR_PAUSE);
				nextNumber = element.getAttribute("value");
			}
			else {
				logger.warn(className + " is not a number pattern field");
			}
		}
		
		return nextNumber;
	}
	
	protected void chooseNextNumber(WebElement numberPatternChooserElement) {
		chooseNextNumber(numberPatternChooserElement, null);
	}
	
	protected void chooseNextNumber(WebElement numberPatternChooserElement, String patternName) {
		WebElement buttonElement = null;

		try {
			buttonElement = driver.findElement(By.className("csc-numberPatternChooser-button"));
		}
		catch(NoSuchElementException e) {
			logger.warn("button not found for number pattern chooser");
		}

		if (buttonElement != null) {
			buttonElement.click();
			
			List<WebElement> candidatePatternElements = buttonElement.findElements(By.xpath("//td[@class=\"csc-numberPatternChooser-name\"]"));
			WebElement patternElement = null;
			
			if (candidatePatternElements.size() > 0) {
				if (patternName != null) {
					for (WebElement candidatePatternElement : candidatePatternElements) {
						if (candidatePatternElement.getText().equals(patternName)) {
							patternElement = candidatePatternElement;
							break;
						}
					}
				}
				else {
					patternElement = candidatePatternElements.get(0);
				}
				
				if (patternElement != null) {
					patternElement.click();
				}
				else {
					logger.warn("no pattern found for name " + patternName);
				}
			}
			else {
				logger.warn("no patterns found for number pattern chooser");
			}
		}
	}
	
	protected void testContactAddressCountryLoads() {
		Assert.assertTrue(isTiedToCountryTermList("csc-contact-addressCountry"), "csc-contact-addressCountry term list should be loaded");
	}
	
	protected boolean isTiedToCountryTermList(String className) {
		try {
			driver.findElement(By.cssSelector("select." + className + " option[value^=\"urn:cspace:cinefiles.cspace.berkeley.edu:vocabularies:name(country)\"]"));
		}
		catch(NoSuchElementException e) {
			logger.warn("no field found for class " + className);
			
			return false;
		}
		
		return true;
	}
	
	protected void closeMessageBar() {
		try {		
			driver.findElement(By.className("csc-messageBar-cancel")).click();
		}
		catch(NoSuchElementException e) {
			logger.warn("failed to find message bar cancel button");
		}
	}
	
	protected void navigateTo(String url) {
		driver.get(url);
		
		// Pause to let AJAX calls settle down
		pause(PAGE_LOAD_PAUSE);
	}
	
	protected void pause(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {}
	}
}

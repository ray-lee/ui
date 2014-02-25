package edu.berkeley.cspace.test;

import static edu.berkeley.cspace.test.UIConstants.*;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.stringtemplate.v4.ST;

import de.svenjacobs.loremipsum.LoremIpsum;

/**
 * @author ray
 *
 */
public class CollectionSpaceDriver {
	public static final Logger logger = Logger.getLogger(CollectionSpaceDriver.class);

	public static final String DEFAULT_BASE_URL = "http://localhost:8180";
	public static final String DEFAULT_TENANT_NAME = "core";

	public static final String DEFAULT_USER = "admin@core.collectionspace.org";
	public static final String DEFAULT_PASSWORD = "Administrator";
	public static final Page DEFAULT_LANDING_PAGE = Page.FIND_EDIT;

	public static final String BASE_PATH = "/collectionspace/ui/<this.tenantName>/html/";
	
	public static final long DEFAULT_TIMEOUT = 5;
	public static final long SAVE_TIMEOUT = 10;
	
	public static final long PAGE_LOAD_PAUSE = 2;
	public static final long NUMBER_GENERATOR_PAUSE = 1;
	public static final long ADD_TERM_PAUSE = 1;
	
	private WebDriver driver;
	
	private String baseUrl = DEFAULT_BASE_URL;
	private String tenantName = DEFAULT_TENANT_NAME;
	private String user = DEFAULT_USER;
	private String password = DEFAULT_PASSWORD;
	private Page landingPage = DEFAULT_LANDING_PAGE;

	public CollectionSpaceDriver() {
		createWebDriver();
		configureWebDriver();
	}
	
	public void createWebDriver() {
		driver = new FirefoxDriver();
	}
	
	public void configureWebDriver() {
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Page getLandingPage() {
		return landingPage;
	}
	
	public void setLandingPage(Page landingPage) {
		this.landingPage = landingPage;
	}

	public void close() {
		driver.close();
	}
	
	public void login() throws LoginFailedException {
		login(getUser(), getPassword());
	}
	
	public void login(String user, String password) throws LoginFailedException {
		navigateTo(Page.LOGIN);

		driver.findElement(By.className(USER_FIELD_CLASSNAME)).sendKeys(user);
		driver.findElement(By.className(PASSWORD_FIELD_CLASSNAME)).sendKeys(password);
		driver.findElement(By.className(LOGIN_BUTTON_CLASSNAME)).click();

		List<WebElement> logoutElements = driver.findElements(By.className(LOGOUT_FORM_CLASSNAME));

		if (logoutElements.size() == 0) {
			String error = findErrorMessageImmediately();
			
			throw new LoginFailedException("login failed with error: " + error);
		}
	}

	public String findErrorMessageWithTimeout(int timeoutSeconds) {
		String errorMessage = null;
		WebElement errorMessageElement = null;
		
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		List<WebElement> errorMessageElements = driver.findElements(By.cssSelector(".csc-messageBar-container[style=\"display: block;\"] .cs-message-error #message"));
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		
		if (errorMessageElements.size() > 0) {
			errorMessageElement = errorMessageElements.get(0);
		}
		
		if (errorMessageElement != null) {
			errorMessage = errorMessageElement.getText();
		}
		
		return errorMessage;
		
	}
	
	public String findErrorMessageImmediately() {
		return findErrorMessageWithTimeout(0);
	}
	
	public void closeMessageBar() {
		try {		
			findElementImmediately(By.className("csc-messageBar-cancel")).click();
		}
		catch(NoSuchElementException e) {
			logger.warn("failed to find message bar cancel button");
		}
	}
	
	public void navigateTo(Page page) {
		String url = getUrlTo(page);
		
		logger.debug("navigating to " + url);
		
		driver.get(url);
		
		waitForSpinner();
		waitForTermLists();
	}

	/**
	 * Blocks until the spinner (loading indicator) is no longer visible.
	 */
	public void waitForSpinner() {
		logger.debug("waiting for spinner");

		// This is a bit brittle, but the best I could come up with.
		// When hidden, the spinner element has "display: none;" in its style
		// attribute. Use the CSS3 *= selector for this, which Selenium/Firefox
		// appears to support.
		
		driver.findElement(By.cssSelector("." + LOADING_INDICATOR_CLASSNAME + "[style*=\"display: none;\"]"));		
	}
	
	/**
	 * Blocks until all dynamic term lists on the page have loaded.
	 */
	public void waitForTermLists() {
		logger.debug("waiting for term lists");
		
		List<WebElement> termLists = findElementsImmediately(By.className("cs-termList"));
		int termListCount = termLists.size();
		int loadedTermListCount = 0;
		
		logger.debug("found " + termListCount + " term lists");
		
		// Once a dynamic term list has been loaded, it will contain a select
		// with name="termList-selection".
		
		if (termLists.size() > 0) {
			List<WebElement> loadedTermLists;
	
			do {
				loadedTermLists = driver.findElements(By.cssSelector(".cs-termList select[name=\"termList-selection\"]"));

				if (loadedTermLists.size() > loadedTermListCount) {
					loadedTermListCount = loadedTermLists.size();
					logger.debug(loadedTermListCount + " of " + termListCount + " term lists loaded");
				}
			}
			while(loadedTermListCount < termListCount);
		}
	}

	public void expandAllSections() {
		expandAllSections(driver.findElement(By.tagName("body")));
	}
	
	/**
	 * Expands all collapsible sections within the context element.
	 */
	public void expandAllSections(WebElement context) {
		List<WebElement> collapsedTogglableElements = findElementsImmediately(context, By.cssSelector("div.cs-togglable-header.cs-togglable-collapsed"));

		if (collapsedTogglableElements.size() > 0) {
			logger.debug("expanding " + collapsedTogglableElements.size() + " collapsed sections");
			
			for (WebElement element : collapsedTogglableElements) {
				element.click();
			}
		}
		else {
			logger.debug("no collapsed sections found");
		}
	}

	public WebElement findElementWithTimeout(int timeoutSeconds, By by) {
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		WebElement foundElement = driver.findElement(by);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		
		return foundElement;
	}
	
	/**
	 * Find element without waiting.
	 * 
	 * @param by	The locating mechanism to use
	 * @return		The first matching element on the current page
	 */
	public WebElement findElementImmediately(By by) {
		return findElementWithTimeout(0, by);
	}

	public List<WebElement> findElementsWithTimeout(int timeoutSeconds, By by) {
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		List<WebElement> foundElements = driver.findElements(by);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		
		return foundElements;
	}

	/**
	 * Find elements without waiting.
	 * 
	 * @param by	The locating mechanism to use
	 * @return		A list of all WebElements, or an empty list if nothing matches
	 */
	public List<WebElement> findElementsImmediately(By by) {
		return findElementsWithTimeout(0, by);
	}

	public WebElement findElementWithTimeout(int timeoutSeconds, WebElement context, By by) {
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		WebElement foundElement = context.findElement(by);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		
		return foundElement;
	}

	/**
	 * Find element without waiting.
	 * 
	 * @param by	The locating mechanism to use
	 * @return		The first matching element on the current page
	 */
	public WebElement findElementImmediately(WebElement context, By by) {
		return findElementWithTimeout(0, context, by);
	}

	public List<WebElement> findElementsWithTimeout(int timeoutSeconds, WebElement context, By by) {
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		List<WebElement> foundElements = context.findElements(by);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		
		return foundElements;
	}

	/**
	 * Find elements without waiting.
	 * 
	 * @param context The context element in which to search
	 * @param by      The locating mechanism to use
	 * @return        A list of all WebElements, or an empty list if nothing matches
	 */
	public List<WebElement> findElementsImmediately(WebElement context, By by) {
		return findElementsWithTimeout(0, context, by);
	}
	
	public String getCurrentUrl() {
		return driver.getCurrentUrl();
	}

	public String getLandingPageUrl() {
		return getUrlTo(getLandingPage());
	}
	
	public void pause(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {}
	}
	
	public String getUrlTo(Page page) {
		ST basePathTemplate = new ST(BASE_PATH);
		basePathTemplate.add("this", this);
		
		String basePath = basePathTemplate.render();
		String url = getBaseUrl() + basePath + page.getPath();

		return url; 
	}
	
	/**
	 * Fill all editable fields in a record editor using
	 * generated values.
	 * 
	 * @return
	 */
	public Map<String, Object> fillAllFields() {
		WebElement recordEditor = driver.findElement(By.className("csc-recordEditor-renderer-container"));

		return fillAllFields(recordEditor);
	}
	
	public Map<String, Object> fillAllFields(WebElement context) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		
		for (String className : findAllFields(context)) {
			Object value = fillField(context, className, null);
			values.put(className, value);
		}
		
		return values;
	}
	
	public List<String> findAllFields(WebElement context) {		
		expandAllSections(context);

		logger.debug("finding fields");

		List<WebElement> candidateElements = findElementsImmediately(context, By.cssSelector("input, textarea, select"));
		List<String> fields = new ArrayList<String>();
		
		for (WebElement candidateElement : candidateElements) {
			if (isEditable(candidateElement)) {
				String className = getFieldName(candidateElement);
				fields.add(className);
			}
		}
		
		logger.debug("found " + fields.size() + " fields");
		
		return fields;
	}
	
	/**
	 * Determines if an element represents an editable field. To be
	 * editable, the element must be a text input, a checkbox, a
	 * select, or a textarea, and it must be visible and enabled.
	 * 
	 * @param element	The element to test
	 * @return			True if the element is an editable field, false otherwise
	 */
	public boolean isEditable(WebElement element) {
		boolean isEditable = false;
		
		if (element.isDisplayed() && element.isEnabled()) {
			String tagName = element.getTagName();

			if (tagName.equals("input")) {
				// Make sure it's a text or checkbox input.
				
				String type = element.getAttribute("type");
				
				if (type.equals("text") || type.equals("checkbox")) {
					isEditable = true;
				}
			}
			else if (tagName.equals("select") || tagName.equals("textarea")) {
				isEditable = true;
			}
		}
		
		return isEditable;
	}
	
	/**
	 * Given an element that represents an editable field, finds the 
	 * name of the field. This is the class name that identifies the
	 * field, suitable for passing in to fillField.
	 * 
	 * @param element   The element
	 * @return			The identifying class name of the field, or null if the
	 *        			name can not be determined
	 */
	public String getFieldName(WebElement element) {
		String fieldName = null;
		String className = element.getAttribute("class");
		String tagName = element.getTagName();
		
		if (className != null && className.equals("cs-autocomplete-input")) {
			// This is an autocomplete. The field name is attached
			// to the previous sibling of the editable input.
			
			element = findPreviousSiblingElementByTag(element, "input");
			className = element.getAttribute("class");
		}
		
		if (!tagName.equals("select") && !className.contains(" ")) {
			// If the element is not a select and there is only one class, 
			// the class is the name of the field.  This is the case for
			// fields in the structured date popup.
			
			fieldName = className;
		}
		else {
			// There is more than one class, and it's not obvious which
			// one is the field name. Try to get it from the name.
			
			String name = element.getAttribute("name");
	
			if (name != null) {
				if (tagName.equals("select") && 
						(name.equals("termList-selection") || name.contains("dateEarliestSingle") || name.contains("dateLatest"))) {
					// This is a dynamic term list, or a static term list
					// in the structured date dialog. Getting the name is
					// going to be tricky. The first class usually
					// seems to be it.
					
					if (className != null) {
						String[] classNames = className.split("\\s");
						
						if (classNames.length > 0) {
							fieldName = classNames[0];
						}
					}
				}
				else {
					// The name has the following possible forms:
					//   .csc-object-identification-number-objects
					//   repeat::.csc-object-identification-brief-description
					//   .csc-collection-object-docTitleArticle-selection
					//   repeat::.csc-object-description-content-object-type-selection
					
					fieldName = name.substring(name.indexOf('.') + 1);
					
					if (fieldName.endsWith("-selection")) {
						// Static term list selects have "-selection" appended
						// to the name.
												
						if (tagName.equals("select")) {
							fieldName = fieldName.substring(0, fieldName.length() - "-selection".length());
						}
					}
				}
			}
		}
		
		return fieldName;
	}

	public Object fillField(String className, Object value) {
		return fillField(driver.findElement(By.tagName("body")), className, value);
	}
	
	/**
	 * Fills a field with a given value. If the value is null,
	 * the field is filled with a generated, non-empty value.
	 * 
	 * @param context    The element in which to locate the field
	 * @param className	 The identifying class name of the field
	 * @param value      The value to place in the field, or null
	 *                   to generate a non-empty value
	 * @return           The value placed in the field
	 */
	public Object fillField(WebElement context, String className, Object value) {
		logger.debug("filling field " + className);

		WebElement element = null;	
		List<WebElement> elements = findElementsImmediately(context, By.className(className));
		
		if (elements.size() == 0) {
			logger.warn("no field found for class " + className);
		}
		else {
			if (elements.size() > 1) {
				logger.warn("multiple fields found for class " + className);
			}

			element = elements.get(0);
		}
		
		if (element != null) {
			if (isCheckbox(element)) {
				value = fillCheckbox(element, (String) value);
			}
			else if (isSelect(element)) {
				value = fillSelectFieldByDisplayValue(element, (String) value);
			}
			else if (isAutocomplete(element)) {
				value = fillAutocompleteField(element, (String) value);
			}
			else if (isStructuredDate(element)) {
				value = fillStructuredDateField(element, (Map<String, String>) value);
			}
			else if (isText(element)) {
				value = fillTextField(element, (String) value);
			}
			else {
				logger.warn("unknown field type for class " + className);
			}
		}
		
		return value;
	}

	public boolean isCheckbox(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equals("input") && element.getAttribute("type").equals("checkbox"));
	}
	
	public boolean isSelect(WebElement element) {
		String tagName = element.getTagName();

		return tagName.equals("select");
	}
	
	public boolean isAutocomplete(WebElement element) {
		boolean isAutocomplete = false;
		
		if (isText(element)) {
			WebElement autocompleteInputElement = findFollowingSiblingAutocompleteInputElement(element);
			
			if (autocompleteInputElement != null) {
				isAutocomplete = true;
			}
		}
		
		return isAutocomplete;
	}
	
	public boolean isNumberPattern(WebElement element) {
		boolean isNumberPattern = false;
		
		if (isText(element)) {
			WebElement chooserElement = findFollowingSiblingNumberPatternChooserElement(element);
			
			if (chooserElement != null) {
				isNumberPattern = true;
			}
		}
		
		return isNumberPattern;
	}

	public boolean isText(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equalsIgnoreCase("textarea") || (tagName.equalsIgnoreCase("input") && element.getAttribute("type").equalsIgnoreCase("text")));
	}
	
	public boolean isMultilineText(WebElement element) {
		String tagName = element.getTagName();
		
		return (tagName.equalsIgnoreCase("textarea"));
	}
	
	/**
	 * Heuristically determines if a text field is intended to
	 * hold a number.
	 * 
	 * @param element The text input element
	 * @return        True if it's likely that the field value should be a number,
	 *                false otherwise
	 */
	public boolean isNumericText(WebElement element) {
		final List<String> expectedNames = Arrays.asList("value", "number", "count");
		
		String className = element.getAttribute("class").toLowerCase();
		String name = element.getAttribute("name").toLowerCase();
		
		boolean foundMatch = false;
		
		for(String expectedName : expectedNames) {
			if (name.contains(expectedName) || className.contains(expectedName)) {
				foundMatch = true;
				break;
			}
		}
		
		return foundMatch;
	}
	
	/**
	 * Heuristically determines if a text field is intended to
	 * hold a year.
	 * 
	 * @param element The text input element
	 * @return        True if it's likely that the field value should be a year,
	 *                false otherwise
	 */
	public boolean isYearText(WebElement element) {
		final String expectedName = "year";
		
		String className = element.getAttribute("class").toLowerCase();
		String name = element.getAttribute("name").toLowerCase();
		
		return (name.contains(expectedName) || className.contains(expectedName));
	}
	
	/**
	 * Heuristically determines if a text field is intended to
	 * hold a month.
	 * 
	 * @param element The text input element
	 * @return        True if it's likely that the field value should be a month,
	 *                false otherwise
	 */
	public boolean isMonthText(WebElement element) {
		final String expectedName = "month";
		
		String className = element.getAttribute("class").toLowerCase();
		String name = element.getAttribute("name").toLowerCase();
		
		return (name.contains(expectedName) || className.contains(expectedName));
	}
	
	/**
	 * Heuristically determines if a text field is intended to
	 * hold a day of the month.
	 * 
	 * @param element The text input element
	 * @return        True if it's likely that the field value should be a day,
	 *                false otherwise
	 */
	public boolean isDayText(WebElement element) {
		final String expectedName = "day";
		
		String className = element.getAttribute("class").toLowerCase();
		String name = element.getAttribute("name").toLowerCase();
		
		return (name.contains(expectedName) || className.contains(expectedName));
	}
	
	public boolean isCalendarDate(WebElement element) {
		return element.getAttribute("class").contains("csc-calendar-date");
	}
	
	public boolean isStructuredDate(WebElement element) {
		return element.getAttribute("class").contains("cs-structuredDate-input");
	}
	
	/**
	 * Fills a checkbox with a given value. The value is interpreted
	 * as a boolean, via Boolean.parseBoolean. If the value is null,
	 * a generated, non-empty value is used.This effectively means 
	 * that a null value results in the checkbox being checked.
	 * 
	 * @param element The checkbox element
	 * @param text    The new value, or null to generate a non-empty value
	 * @return        The value placed in the field, converted from a boolean
	 *                using Boolean.toString
	 */
	public String fillCheckbox(WebElement element, String value) {
		if (value == null) {
			value = generateCheckboxValue();
		}
		
		boolean newCheckedState = Boolean.parseBoolean(value);
		
		String checked = element.getAttribute("checked");
		boolean currentCheckedState = (checked != null);
		
		if (currentCheckedState != newCheckedState) {
			element.click();
		}
		
		return Boolean.toString(newCheckedState);
	}
	
	/**
	 * Fills a select field with a given value. If the value is null,
	 * a generated, non-empty value is used.
	 * 
	 * @param element The select element
	 * @param text    The new value, or null to generate a non-empty value
	 * @return        The value placed in the field
	 */
	public String fillSelectFieldByDisplayValue(WebElement element, String value) {
		if (value == null) {
			value = generateSelectValue(element);
		}
		
		WebElement optionElement = null;
		
		for (WebElement candidateOptionElement : findElementsImmediately(element, By.tagName("option"))) {
			if (candidateOptionElement.getText().equals(value)) {
				optionElement = candidateOptionElement;
				break;
			}
		}
		
		if (optionElement == null) {
			logger.warn("no option found in select for display value " + value);
		}
		else {
			element.click();
			optionElement.click();
			element.sendKeys("\n");
		}
		
		return value;
	}

	public void fillSelectFieldByValue(String className, String value) {
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
	
	public void fillSelectFieldByValue(WebElement element, String value) {
		WebElement optionElement = null;
		
		for (WebElement candidateOptionElement : findElementsImmediately(element, By.tagName("option"))) {
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
	
	/**
	 * Fills an autocomplete field with a given value. If the value is null,
	 * a generated, non-empty value is used.
	 * 
	 * @param element The autocomplete input element
	 * @param text    The new value, or null to generate a non-empty value
	 * @return        The value placed in the field
	 */
	public String fillAutocompleteField(WebElement element, String value) {		
		if (value == null) {
			value = generateAutocompleteValue();
		}
		
		WebElement autocompleteInputElement = findFollowingSiblingAutocompleteInputElement(element);
		
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
				
				// Check for a confirmation dialog. This may appear when
				// setting object hierarchy fields.
				
				List<WebElement> confirmationDialogs = findElementsWithTimeout(1, By.cssSelector(".ui-dialog[style*=\"display: block\"] .cs-confirmationDialog"));

				if (confirmationDialogs.size() > 0) {
					WebElement confirmationDialog = confirmationDialogs.get(0);
					WebElement saveButton = findElementImmediately(confirmationDialog, By.className("saveButton"));
					
					saveButton.click();
				}
			}
			else {
				logger.debug("adding term " + value);

				WebElement addToPanelElement = popupElement.findElement(By.className("csc-autocomplete-addToPanel"));
				WebElement firstAuthorityItem = addToPanelElement.findElement(By.tagName("li"));
				
				firstAuthorityItem.click();
				
				while(findElementsImmediately(By.className("cs-autocomplete-popup")).size() > 0) {
					// Wait for the popup to close
				}
			}
		}
		else {
			logger.warn("could not find autocomplete input");
		}
		
		return value;
	}
	
	public Map<String, String> fillStructuredDateField(WebElement element, Map<String, String> values) {
		element.click();

		// Wait for term lists in the structured date popup to load
		waitForTermLists();

		WebElement popupContainer = findFollowingSiblingElementByClass(element, "csc-structuredDate-popup-container");
		Map<String, String> filledValues = new LinkedHashMap<String, String>();
		
		for (String className : findAllFields(popupContainer)) {
			String value = null;
			
			if (values != null) {
				value = values.containsKey(className) ? values.get(className) : "";
			}
			
			String filledValue = (String) fillField(popupContainer, className, value);			
			filledValues.put(className, filledValue);
		}
		
		return (values == null ? filledValues : values);
	}
	
	/**
	 * Fills a text input field with a given value. If the value is null,
	 * a generated, non-empty value is used.
	 * 
	 * @param element The text input element
	 * @param text    The new value, or null to generate a non-empty value
	 * @return        The value placed in the field
	 */
	public String fillTextField(WebElement element, String value) {
		boolean generated = false;
		
		if (value == null) {
			if (isCalendarDate(element)) {
				value = generateCalendarDateValue();
			}
			else if (isStructuredDate(element)) {
				value = generateCalendarDateValue();
			}
			else if (isNumberPattern(element)) {
				value = chooseNextNumber(element);
				
				// chooseNextNumber had the side effect of filling in
				// the field. Clear it now, so that the value won't
				// be typed in twice.
				
				element.clear();
			}
			else {
				// Structured date fields are validated, but not until save,
				// at which point it's impossible to tell from the error message 
				// exactly which field caused the error. So special case these,
				// to make sure a valid value is entered.
				
				if (isYearText(element)) {
					value = generateYearValue();
				}
				else if (isMonthText(element)) {
					value = generateMonthValue();
				}
				else if (isDayText(element)) {
					value = generateDayValue();
				}
				else if (isNumericText(element)) {
					value = generateNumericValue();
				}
				else {
					value = generateTextValue(isMultilineText(element));
				}
			}
			
			generated = true;
		}
		
		element.click();
		element.sendKeys(value);
		element.sendKeys("\t");
		
		if (generated) {
			// If the value was generated, make sure there were no
			// validation errors. Allow 1 second for the error to
			// appear.
			
			String error = findErrorMessageWithTimeout(1);
			
			if (error != null) {
				if (error.contains("number you have entered is invalid")) {
					// The field requires a number.
					
					closeMessageBar();
					
					value = generateNumericValue();

					element.click();
					element.clear();
					element.sendKeys(value);
					element.sendKeys("\t");
				}
				else {
					// Some other error.
					
					logger.warn("unexpected field validation error: " + error);
				}
			}
		}
		
		return value;
	}

	protected WebElement findFollowingSiblingAutocompleteInputElement(WebElement element) {
		return findFollowingSiblingElementByClass(element, "cs-autocomplete-input");
	}
	
	protected WebElement findFollowingSiblingNumberPatternChooserElement(WebElement element) {
		return findFollowingSiblingElementByClass(element, "cs-numberPatternChooserContainer");
	}
	
	protected WebElement findFollowingSiblingElementByClass(WebElement element, String className) {
		WebElement foundElement = null;
		
		List<WebElement> foundElements = findElementsImmediately(element, By.xpath("following-sibling::*[@class=\"" + className + "\"]"));
		
		if (foundElements.size() > 0) {
			foundElement = foundElements.get(0);
		}
		
		return foundElement;	
	}

	/**
	 * Generates a non-empty value to fill a select field.
	 * The value returned is the display value of an option
	 * in the select that has a non-empty data value.
	 * 
	 * @param element The select element
	 * @return        A non-empty value
	 */
	public String generateSelectValue(WebElement element) {
		List<String> displayValues = new ArrayList<String>();
		
		for (WebElement optionElement : findElementsImmediately(element, By.tagName("option"))) {
			String dataValue = optionElement.getAttribute("value");
			
			if (StringUtils.isNotEmpty(dataValue)) {
				displayValues.add(optionElement.getText());
			}
		}
		
		// Choose an option randomly.
		
		int index = (int) Math.floor(Math.random() * displayValues.size());
		
		return displayValues.get(index);
	}
	
	/**
	 * Generates a non-empty value to fill a checkbox field. "Non-empty"
	 * effectively means that this function must return a true (checked)
	 * value. This may change, if the CollectionSpace UI ever begins to
	 * use tri-state checkboxes.
	 * 
	 * @return A non-empty value
	 */
	public String generateCheckboxValue() {
		return Boolean.toString(true);
	}
	
	/**
	 * Generates a non-empty value to fill a text input field.
	 * 
	 * @param multiline If true, generate a multi-line value
	 * @return          A non-empty value
	 */
	public String generateTextValue(boolean multiline) {
		LoremIpsum generator = new LoremIpsum();
		List<String> lines = new ArrayList<String>();
		
		int numLines;
		
		if (multiline) {
			numLines = (int) Math.floor(Math.random() * 5) + 1;
		}
		else {
			numLines = 1;
		}
		
		for (int i=0; i<numLines; i++) {
			int numWords = (int) Math.floor(Math.random() * 5) + 1;
			int startIndex = (int) Math.floor(Math.random() * 50);
			
			lines.add(generator.getWords(numWords, startIndex));
		}
		
		return StringUtils.capitalize(StringUtils.join(lines, "\n"));
	}
	
	/**
	 * Generates a non-empty value to fill a numeric input field.
	 * 
	 * @return          A non-empty value
	 */
	public String generateNumericValue() {
		// Return a random number between 1 and 200.

		int number = (int) Math.floor(Math.random() * 200) + 1;

		return Integer.toString(number);
	}
	
	/**
	 * Generates a non-empty value to fill an autocomplete field.
	 * 
	 * @return          A non-empty value
	 */
	public String generateAutocompleteValue() {
		// Return "Test " followed by a random number between 1 and 20.

		int number = (int) Math.floor(Math.random() * 20) + 1;

		return ("Test " + number);
	}
	
	/**
	 * Generates a non-empty value to fill a calendar date field.
	 * 
	 * @return          A non-empty value
	 */
	public String generateCalendarDateValue() {
		// Return a random date between today and about 100 years ago.
		
		int days = (int) Math.floor(Math.random() * 100 * 365);
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -days);
		
		return DateFormat.getDateInstance().format(calendar.getTime());		
	}
	
	public String generateYearValue() {
		// Return a random number between 1900 and 2100.
		
		int year = 1900 + (int) Math.floor(Math.random() * 200);
		
		return Integer.toString(year);
	}

	public String generateMonthValue() {
		// Return a random number between 1 and 12.
		
		int month = (int) Math.floor(Math.random() * 12) + 1;
		
		return Integer.toString(month);
	}
	
	public String generateDayValue() {
		// Return a random number between 1 and 28.
		// (Numbers above 28 may not be valid, depending on the year and month).
		
		int day = (int) Math.floor(Math.random() * 28) + 1;
		
		return Integer.toString(day);
	}
	
	/*
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
	
	
	
 */
	
	public WebElement findPreviousSiblingElementByTag(WebElement element, String tagName) {
		WebElement foundElement = null;
		
		// We want the immediately preceding sibling. If there's more than one, it's the
		// last, so we have to find them all and pick the last.
		
		List<WebElement> precedingSiblings = findElementsImmediately(element, By.xpath("preceding-sibling::" + tagName));
		
		if (precedingSiblings.size() > 0) {
			foundElement = precedingSiblings.get(precedingSiblings.size() - 1);
		}
		
		return foundElement;			
	}
	
//	protected String chooseNextNumber(String className) {
//		return chooseNextNumber(className, null);
//	}
	
//	protected String chooseNextNumber(String className, String patternName) {
//		WebElement element = null;
//		String nextNumber = "";
//		
//		try {
//			element = driver.findElement(By.className(className));
//		}
//		catch(NoSuchElementException e) {
//			logger.warn("no field found for class " + className);
//		}
//		
//		if (element != null) {
//			if (isNumberPattern(element)) {
//				chooseNextNumber(findSiblingNumberPatternChooserElement(element), patternName);
//				pause(NUMBER_GENERATOR_PAUSE);
//				nextNumber = element.getAttribute("value");
//			}
//			else {
//				logger.warn(className + " is not a number pattern field");
//			}
//		}
//		
//		return nextNumber;
//	}
	
	protected String chooseNextNumber(WebElement numberPatternChooserElement) {
		return chooseNextNumber(numberPatternChooserElement, null);
	}
	
	protected String chooseNextNumber(WebElement numberPatternChooserElement, String patternName) {
		String newNumber = null;
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
					// Clear the field first, so it will be possible to tell when
					// the new number has been filled in.
					
					numberPatternChooserElement.clear();
					patternElement.click();
					
					String value = "";
					
					while(StringUtils.isEmpty(value)) {
						value = numberPatternChooserElement.getAttribute("value");
					}
					
					newNumber = value;
				}
				else {
					logger.warn("no pattern found for name " + patternName);
				}
			}
			else {
				logger.warn("no patterns found for number pattern chooser");
			}
		}
		
		return newNumber;
	}
	
	/*
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
	
	protected void pause(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {}
	}
	*/
}

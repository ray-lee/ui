package edu.berkeley.cspace.test;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * The base CollectionSpace integration test.
 */
public class CollectionSpaceIT {
	public static final Logger logger = Logger.getLogger(CollectionSpaceIT.class);
	
	public static final String TENANT_NAME_PROPERTY = "tenantName";
	public static final String USER_PROPERTY = "user";
	public static final String PASSWORD_PROPERTY = "password";
	
	protected CollectionSpaceDriver driver;

	@BeforeClass
	public void setUp() {
		driver = new CollectionSpaceDriver();
		
		Properties properties = System.getProperties();

		if (properties.containsKey(TENANT_NAME_PROPERTY)) {
			driver.setTenantName(System.getProperty(TENANT_NAME_PROPERTY));
		}
		
		if (properties.containsKey(USER_PROPERTY)) {
			driver.setUser(System.getProperty(USER_PROPERTY));
		}

		if (properties.containsKey(PASSWORD_PROPERTY)) {
			driver.setPassword(System.getProperty(PASSWORD_PROPERTY));
		}
	}

	@AfterClass
	public void finish() {
		//driver.close();
	}
	
	/**
	 * Logs in to the tenant.
	 * <ul>
	 * <li>Login should succeed</li>
	 * </ul>
	 */
	@Test
	public void testLogin() {		
		try {
			driver.login();
		}
		catch(LoginFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}

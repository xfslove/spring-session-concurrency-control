package spring.session.concurrent;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by hanwen on 15-8-6.
 */
public class ConfigDataProviderTest {
	
	@Test
	public void testObject() {
		ConfigDataProvider configDataProvider = new ConfigDataProvider();
		assertEquals("principal", configDataProvider.getPrincipalAttr());
		assertEquals(1, configDataProvider.getMaximumSessions());
		assertNull(configDataProvider.getTargetUrl());

		configDataProvider.setPrincipalAttr("my-principal");
		configDataProvider.setMaximumSessions(2);
		configDataProvider.setTargetUrl("/login");

		assertEquals("my-principal", configDataProvider.getPrincipalAttr());
		assertEquals(2, configDataProvider.getMaximumSessions());
		assertEquals("/login", configDataProvider.getTargetUrl());
	}
}
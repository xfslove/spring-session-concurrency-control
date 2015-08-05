package spring.session.concurrent;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SessionInformationTests {

	@Test
	public void testObject() throws Exception {
		String principal = "Some principal object";
		String sessionId = "1234567890";
		Long currentDate = System.currentTimeMillis();

		SessionInformation info = new SessionInformation(principal, sessionId,
				currentDate);
		assertEquals(principal, info.getPrincipal());
		assertEquals(sessionId, info.getSessionId());
		assertEquals(currentDate, info.getLastRequest());
	}
}
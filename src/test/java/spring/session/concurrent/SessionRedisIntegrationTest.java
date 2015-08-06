package spring.session.concurrent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

@WebAppConfiguration
@SpringApplicationConfiguration(classes = SessionRedisIntegrationTest.class)
@ComponentScan(basePackages = {"spring.session.concurrent"})
public class SessionRedisIntegrationTest<S extends Session> extends AbstractTestNGSpringContextTests {
	@Autowired
	private SessionRepository<S> repository;

	@Autowired
	private SessionInformationRepository sessionInformationRepository;

	@Autowired
	private SessionDestroyedEventRegistry sessionDestroyedEventRegistry;

	private final Object lock = new Object();

	@BeforeMethod(alwaysRun = true)
	public void setup() {
		sessionDestroyedEventRegistry.setLock(lock);
	}

	@Test
	public void testInformationSave() throws InterruptedException {

		S toSave = repository.createSession();
		toSave.setAttribute("principal", "user");
		repository.save(toSave);

		sessionInformationRepository.registerNewSessionInformation(toSave.getId(), toSave.getAttribute("principal").toString());

		SessionInformation sessionInformation = sessionInformationRepository.getSessionInformation(toSave.getId(), toSave.getAttribute("principal").toString());

		assertEquals(sessionInformation.getSessionId(), toSave.getId());
		assertNotNull(sessionInformation.getLastRequest());
		assertEquals(sessionInformation.getPrincipal(), toSave.getAttribute("principal").toString());
		assertEquals(sessionInformation.isExpired(), false);

		repository.delete(toSave.getId());

		synchronized (lock) {
			lock.wait(3000);
		}
		assertTrue(sessionDestroyedEventRegistry.receivedEvent());
		assertNull(sessionInformationRepository.getSessionInformation(toSave.getId(), toSave.getAttribute("principal").toString()));
	}

	static class SessionDestroyedEventRegistry implements ApplicationListener<SessionDestroyedEvent> {
		private boolean receivedEvent;
		private Object lock;

		@Override
		public void onApplicationEvent(SessionDestroyedEvent event) {
			receivedEvent = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		public boolean receivedEvent() {
			return receivedEvent;
		}

		public void setLock(Object lock) {
			this.lock = lock;
		}
	}

	@Configuration
	static class Config {

		@Bean
		public JedisConnectionFactory connectionFactory() throws Exception {
			JedisConnectionFactory factory = new JedisConnectionFactory();
			factory.setUsePool(false);
			return factory;
		}

		@Bean
		public SessionDestroyedEventRegistry sessionDestroyedEventRegistry() {
			return new SessionDestroyedEventRegistry();
		}
	}
}
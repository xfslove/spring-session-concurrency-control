package spring.session.concurrent;

import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * Created by hanwen on 15-8-4.
 */
public class SessionRepositoryTests {

	@Mock
	RedisOperations redisOperations;

	@Mock
	BoundHashOperations<String, Object, Object> boundHashOperations;

	@Mock
	BoundSetOperations<String, String> boundSetOperations;

	private SessionRepository sessionRepository;

	String INFORMATION_BOUNDED_HASH_KEY_PREFIX = "spring:session:information:";

	String PRINCIPAL_BOUNDED_HASH_KEY_PREFIX = "spring:session:principal:";

	@BeforeMethod(alwaysRun = true)
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.sessionRepository = new SessionRepository(redisOperations);
	}

	@Test
	public void getAllPrincipalsTest() {
		Set<String> keys = new HashSet<String>();
		keys.add(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + "user1");
		keys.add(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + "user2");
		when(redisOperations.keys(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + "*")).thenReturn(keys);
		List<String> result = sessionRepository.getAllPrincipals();
		assertEquals("user1", result.get(1));
		assertEquals("user2", result.get(0));
	}

	@Test
	public void getAllSessionsTest() {
		String principal = "user1";
		String sessionUsedByPrincipal = "12344-23432-42342-43543";
		when(redisOperations.boundSetOps(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + principal)).thenReturn(boundSetOperations);
		Set<String> principals = new HashSet<String>();
		principals.add(sessionUsedByPrincipal);
		when(boundSetOperations.members()).thenReturn(principals);
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionUsedByPrincipal)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", "");
		map.put("sessionId", sessionUsedByPrincipal);
		when(boundHashOperations.entries()).thenReturn(map);
		List<SessionInformation> results = sessionRepository.getAllSessions(principal, true);
		assertEquals(sessionUsedByPrincipal, results.get(0).getSessionId());
	}

	@Test
	public void refreshLastRequestTest() {
		String sessionId = "12344-23432-42342-43543";
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", "");
		map.put("sessionId", sessionId);

		when(boundHashOperations.entries()).thenReturn(map);
		sessionRepository.refreshLastRequest(sessionId);
		verify(boundHashOperations).delete("lastRequest");
		ArgumentMatcher<String> argumentMatcher1 = new ArgumentMatcher<String>() {

			@Override
			public boolean matches(Object argument) {
				return "lastRequest".equals(argument);
			}
		};
		ArgumentMatcher<Long> argumentMatcher2 = new ArgumentMatcher<Long>() {

			@Override
			public boolean matches(Object argument) {
				return (Long) argument != 0l;
			}
		};
		verify(boundHashOperations).put(argThat(argumentMatcher1), argThat(argumentMatcher2));
	}

	@Test
	public void expireNowTest() {
		String sessionId = "12344-23432-42342-43543";
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", "");
		map.put("sessionId", sessionId);
		when(boundHashOperations.entries()).thenReturn(map);
		sessionRepository.expireNow(sessionId);
		verify(boundHashOperations).delete("expired");
		verify(boundHashOperations).put("expired", true);
	}

	@Test
	public void registerNewSessionInformationNullTest() {
		String principal = "user1";
		String sessionId = "12344-23432-42342-43543";
		when(redisOperations.boundSetOps(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + principal)).thenReturn(boundSetOperations);
		Set<String> principals = new HashSet<String>();
		principals.add(sessionId);
		when(boundSetOperations.members()).thenReturn(principals);
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", "");
		map.put("sessionId", sessionId);
		when(boundHashOperations.entries()).thenReturn(map);
		sessionRepository.registerNewSessionInformation(sessionId, principal);
	}

	@Test
	public void registerNewSessionInformationNotNullTest() {
		final String principal = "user1";
		String sessionId = "12344-23432-42342-43543";
		when(redisOperations.boundSetOps(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + principal)).thenReturn(boundSetOperations);
		Set<String> principals = new HashSet<String>();
		principals.add(sessionId);
		when(boundSetOperations.members()).thenReturn(principals);
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", "");
		map.put("sessionId", sessionId);
		when(boundHashOperations.entries()).thenReturn(map);
		final String meId = "12344-23432-42342-43544";
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + meId)).thenReturn(boundHashOperations);
		sessionRepository.registerNewSessionInformation(meId, principal);
		verify(boundSetOperations).add(meId);
		Map<Object, Object> meMap = new HashMap<Object, Object>();
		meMap.put("expired", false);
		meMap.put("lastRequest", System.currentTimeMillis());
		meMap.put("principal", principal);
		meMap.put("sessionId", meId);
		ArgumentMatcher<Map> argumentMatcher = new ArgumentMatcher<Map>() {

			@Override
			public boolean matches(Object argument) {
				Map args = (Map) argument;
				if (false != (Boolean) args.get("expired")) {
					return false;
				}
				if (0l == (Long) args.get("lastRequest")) {
					return false;
				}
				if (!principal.equals(args.get("principal"))) {
					return false;
				}
				if (!meId.equals(args.get("sessionId"))) {
					return false;
				}
				return true;
			}
		};
		verify(boundHashOperations).putAll(argThat(argumentMatcher));
	}

	@Test
	public void removeSessionInformationTest() {
		String principal = "user1";
		String sessionId = "12344-23432-42342-43543";
		when(redisOperations.boundHashOps(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId)).thenReturn(boundHashOperations);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("expired", false);
		map.put("lastRequest", 0l);
		map.put("principal", principal);
		map.put("sessionId", sessionId);
		when(boundHashOperations.entries()).thenReturn(map);
		when(redisOperations.boundSetOps(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + principal)).thenReturn(boundSetOperations);
		Set<String> principals = new HashSet<String>();
		principals.add(principal);
		when(boundSetOperations.members()).thenReturn(principals);
		sessionRepository.removeSessionInformation(sessionId);
		verify(redisOperations).delete(INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId);
		verify(boundSetOperations).remove(sessionId);
	}
}

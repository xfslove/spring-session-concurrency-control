package spring.session.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created by hanwen on 15-7-30.
 */
public class SessionInformationRepository implements ApplicationListener<SessionDestroyedEvent> {

	protected final Log logger = LogFactory.getLog(SessionInformationRepository.class);

	static final String INFORMATION_BOUNDED_HASH_KEY_PREFIX = "spring:session:information:";

	static final String PRINCIPAL_BOUNDED_HASH_KEY_PREFIX = "spring:session:principal:";

	/** <principal,set<sessionId>> */
	private final RedisOperations<String, String> principalRedisOperations;

	/** <sessionId, sessionInformation> */
	private final RedisOperations<String,SessionInformation> informationRedisOperations;

	public SessionInformationRepository(RedisOperations redisOperations) {
		this.principalRedisOperations = redisOperations;
		this.informationRedisOperations = redisOperations;
	}

	String getPrincipalKey(String principal) {
		return PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + principal;
	}

	String getInformationKey(String sessionId) { return INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId; }

	public List<String> getAllPrincipals() {
		Set<String> principalKeys = principalRedisOperations.keys(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + "*");
		if (CollectionUtils.isEmpty(principalKeys)) {
			return Collections.emptyList();
		}
		List<String> principals = new ArrayList<String>();
		for (String principalKey : principalKeys) {
			principals.add(principalKey.substring(PRINCIPAL_BOUNDED_HASH_KEY_PREFIX.length()));
		}
		return principals;
	}


	public List<SessionInformation> getAllSessions(String principal, boolean includeExpiredSessions) {
		String principalKey = getPrincipalKey(principal);
		Set<String> sessionsUsedByPrincipal = principalRedisOperations.boundSetOps(principalKey).members();
		if (CollectionUtils.isEmpty(sessionsUsedByPrincipal)) {
			return Collections.emptyList();
		}
		List<SessionInformation> list = new ArrayList<SessionInformation>(sessionsUsedByPrincipal.size());
		for (String sessionId : sessionsUsedByPrincipal) {
			String informationKey = getInformationKey(sessionId);
			Map<Object, Object> entries = informationRedisOperations.boundHashOps(informationKey).entries();

			boolean expired_attr = false;
			Long lastRequest_attr = 0l;
			String principal_attr = "";
			String sessionId_attr = "";

			for(Map.Entry<Object,Object> entry : entries.entrySet()) {
				String key = (String) entry.getKey();
				if(SessionInformation.EXPIRED_ATTR.equals(key)) {
					expired_attr = (Boolean) entry.getValue();
				} else if(SessionInformation.LAST_REQUEST_ATTR.equals(key)) {
					lastRequest_attr = (Long) entry.getValue();
				} else if(SessionInformation.PRINCIPAL_ATTR.equals(key)) {
					principal_attr = (String) entry.getValue();
				} else if(SessionInformation.SESSION_ID_ATTR.equals(key)) {
					sessionId_attr = (String) entry.getValue();
				}
			}
			SessionInformation sessionInformation = new SessionInformation(principal_attr, sessionId_attr, lastRequest_attr, expired_attr);
			if (includeExpiredSessions || !sessionInformation.isExpired()) {
				list.add(sessionInformation);
			}
		}
		return list;
	}

	public SessionInformation getSessionInformation(String sessionId, String principal) {
		Assert.notNull(sessionId, "Session required as per interface contract");
		Assert.notNull(principal, "Principal required as per interface contract");
		List<SessionInformation> sessions = getAllSessions(principal, true);
		for (SessionInformation si : sessions) {
			if (si.getSessionId().equals(sessionId)) {
				return si;
			}
		}
		return null;
	}

	public void refreshLastRequest(String sessionId) {
		Assert.notNull(sessionId, "Session required as per interface contract");
		String informationKey = getInformationKey(sessionId);
		if (CollectionUtils.isEmpty(informationRedisOperations.boundHashOps(informationKey).entries())) {
			return;
		}
		informationRedisOperations.boundHashOps(informationKey).delete(SessionInformation.LAST_REQUEST_ATTR);
		informationRedisOperations.boundHashOps(informationKey).put(SessionInformation.LAST_REQUEST_ATTR, System.currentTimeMillis());
	}

	public void expireNow(String sessionId) {
		Assert.notNull(sessionId, "Session required as per interface contract");
		String informationKey = getInformationKey(sessionId);
		if (CollectionUtils.isEmpty(informationRedisOperations.boundHashOps(informationKey).entries())) {
			return;
		}
		informationRedisOperations.boundHashOps(informationKey).delete(SessionInformation.EXPIRED_ATTR);
		informationRedisOperations.boundHashOps(informationKey).put(SessionInformation.EXPIRED_ATTR, Boolean.TRUE);
	}

	public void registerNewSessionInformation(String sessionId, String principal) {
		Assert.notNull(sessionId, "Session required as per interface contract");
		Assert.notNull(principal, "Principal required as per interface contract");
		if (logger.isDebugEnabled()) {
			logger.debug("Registering session " + sessionId +", for principal " + principal);
		}
		if (null != getSessionInformation(sessionId, principal)) {
			return;
		}
		// register session to user
		String principalKey = getPrincipalKey(principal);
		principalRedisOperations.boundSetOps(principalKey).add(sessionId);
		// add sessionInformation
		SessionInformation sessionInformation = new SessionInformation(principal, sessionId, System.currentTimeMillis());
		String informationKey = getInformationKey(sessionId);
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(SessionInformation.EXPIRED_ATTR, sessionInformation.isExpired());
		delta.put(SessionInformation.LAST_REQUEST_ATTR, sessionInformation.getLastRequest());
		delta.put(SessionInformation.PRINCIPAL_ATTR, sessionInformation.getPrincipal());
		delta.put(SessionInformation.SESSION_ID_ATTR, sessionInformation.getSessionId());
		informationRedisOperations.boundHashOps(informationKey).putAll(delta);
		if (logger.isTraceEnabled()) {
			logger.trace("Sessions used by '" + sessionInformation.getPrincipal() + "'");
		}
	}

	public void removeSessionInformation(String sessionId) {
		String informationKey = getInformationKey(sessionId);
		Map<Object, Object> entries = informationRedisOperations.boundHashOps(informationKey).entries();
		if (CollectionUtils.isEmpty(entries)) {
			return;
		}
		String principal_attr = "";
		for(Map.Entry<Object,Object> entry : entries.entrySet()) {
			String key = (String) entry.getKey();
			if(SessionInformation.PRINCIPAL_ATTR.equals(key)) {
				principal_attr = (String) entry.getValue();
				break;
			}
		}
		String principalKey = getPrincipalKey(principal_attr);
		Set<String> sessionsUsedByPrincipal = principalRedisOperations.boundSetOps(principalKey).members();
		if (CollectionUtils.isEmpty(sessionsUsedByPrincipal)) {
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.debug("Removing session " + sessionId + " from set of registered sessions");
		}
		informationRedisOperations.delete(informationKey);
		if (logger.isDebugEnabled()) {
			logger.debug("Removing session " + sessionId + " from principal's set of registered sessions");
		}
		principalRedisOperations.boundSetOps(principalKey).remove(sessionId);
		if (logger.isTraceEnabled()) {
			logger.trace("Sessions used by '" + principal_attr + "' : " + sessionsUsedByPrincipal);
		}
	}

	@Override
	public void onApplicationEvent(SessionDestroyedEvent event) {
		this.removeSessionInformation(event.getSessionId());
	}
}

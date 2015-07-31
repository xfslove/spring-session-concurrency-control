package spring.session.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Created by hanwen on 15-7-30.
 */
public class ConcurrentRepository {

	protected final Log logger = LogFactory.getLog(ConcurrentRepository.class);

	static final String INFORMATION_BOUNDED_HASH_KEY_PREFIX = "spring:session:information:";

	static final String PRINCIPAL_BOUNDED_HASH_KEY_PREFIX = "spring:session:principal:";

	static final String VALUE_KEY_PREFIX = "user";

	/** <user,set<sessionId>> */
	private final RedisOperations<String, String> principalRedisOperations;

	/** <sessionId, sessionInformation> */
	private final RedisOperations<String,SessionInformation> informationRedisOperations;

	public ConcurrentRepository(RedisOperations redisOperations) {
		this.principalRedisOperations = redisOperations;
		this.informationRedisOperations = redisOperations;
	}

	String getPrincipalKey(String user) {
		return PRINCIPAL_BOUNDED_HASH_KEY_PREFIX + user;
	}

	String getInformationKey(String sessionId) { return INFORMATION_BOUNDED_HASH_KEY_PREFIX + sessionId; }

	public List<SessionInformation> getAllSessions(HttpSession session, boolean includeExpiredSessions) {
		String principalKey = getPrincipalKey(session.getAttribute(VALUE_KEY_PREFIX).toString());
		Set<String> sessionsUsedByPrincipal = principalRedisOperations.boundSetOps(principalKey).members();
		if (CollectionUtils.isEmpty(sessionsUsedByPrincipal)) {
			return Collections.emptyList();
		}
		List<SessionInformation> list = new ArrayList<SessionInformation>(sessionsUsedByPrincipal.size());
		for (String sessionId : sessionsUsedByPrincipal) {
			String informationKey = getInformationKey(sessionId);
			Map<Object, Object> entries = informationRedisOperations.boundHashOps(informationKey).entries();

			boolean expired = false;
			Long lastRequest = 0l;
			String principal = "";
			String sid = "";

			for(Map.Entry<Object,Object> entry : entries.entrySet()) {
				String key = (String) entry.getKey();
				if(SessionInformation.EXPIRED_ATTR.equals(key)) {
					expired = (Boolean) entry.getValue();
				} else if(SessionInformation.LAST_REQUEST_ATTR.equals(key)) {
					lastRequest = (Long) entry.getValue();
				} else if(SessionInformation.PRINCIPAL_ATTR.equals(key)) {
					principal = (String) entry.getValue();
				} else if(SessionInformation.SESSION_ID_ATTR.equals(key)) {
					sid = (String) entry.getValue();
				}
			}
			SessionInformation sessionInformation = new SessionInformation(principal, sid, lastRequest, expired);
			if (includeExpiredSessions || !sessionInformation.isExpired()) {
				list.add(sessionInformation);
			}
		}
		return list;
	}

	public SessionInformation getSessionInformation(HttpSession session) {
		Assert.notNull(session, "Session required as per interface contract");
		List<SessionInformation> sessions = getAllSessions(session, true);
		for (SessionInformation s : sessions) {
			if (s.getSessionId().equals(session.getId().toString())) {
				return s;
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

	public void registerNewSessionInformation(HttpSession session) {
		Assert.notNull(session, "Session required as per interface contract");
		if (logger.isDebugEnabled()) {
			logger.debug("Registering session " + session.getId() +", for principal " + session.getAttribute(VALUE_KEY_PREFIX));
		}
		if (getSessionInformation(session) != null) {
			removeSessionInformation(session);
		}
		// register session to user
		String principalKey = getPrincipalKey(session.getAttribute(VALUE_KEY_PREFIX).toString());
		principalRedisOperations.boundSetOps(principalKey).add(session.getId().toString());
		// add sessionInformation
		SessionInformation sessionInformation = new SessionInformation(session.getAttribute(VALUE_KEY_PREFIX).toString(), session.getId().toString(), System.currentTimeMillis());
		String informationKey = getInformationKey(session.getId().toString());
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

	public void removeSessionInformation(HttpSession session) {
		Assert.notNull(session, "Session required as per interface contract");
		SessionInformation info = getSessionInformation(session);
		if (info == null) {
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.debug("Removing session " + session.getId() + " from set of registered sessions");
		}
		String informationKey = getInformationKey(info.getSessionId());
		informationRedisOperations.delete(informationKey);
		String principalKey = getPrincipalKey(info.getPrincipal());
		Set<String> sessionsUsedByPrincipal = principalRedisOperations.boundSetOps(principalKey).members();

		if (CollectionUtils.isEmpty(sessionsUsedByPrincipal)) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Removing session " + session.getId() + " from principal's set of registered sessions");
		}
		principalRedisOperations.boundSetOps(principalKey).remove(info.getSessionId());
		if (logger.isTraceEnabled()) {
			logger.trace("Sessions used by '" + info.getPrincipal() + "' : " + sessionsUsedByPrincipal);
		}
	}

	public void cleanExpiredSessions() {
		// TODO delete expired and 30 days ago data

	}
}

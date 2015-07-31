package spring.session.concurrent;


import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * register session information
 * Created by hanwen on 15-7-30.
 */
@Order(ConcurrentRegisterFilter.DEFAULT_ORDER)
public class ConcurrentRegisterFilter extends OncePerRequestFilter {

	/** after {@link org.springframework.session.web.http.SessionRepositoryFilter#DEFAULT_ORDER} */
	public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 51;

	private ConcurrentRepository concurrentRepository;

	private int maximumSessions = 1;

	public ConcurrentRegisterFilter(ConcurrentRepository concurrentRepository) {
		if(concurrentRepository == null) {
			throw new IllegalArgumentException("concurrentRepository cannot be null");
		}
		this.concurrentRepository = concurrentRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(ConcurrentRepository.VALUE_KEY_PREFIX) != null) {
			List<SessionInformation> sessions = concurrentRepository.getAllSessions(session, true);
			boolean register = true;
			for (SessionInformation si : sessions) {
				if (si.getSessionId().equals(session.getId())) {
					register = false;
					break;
				}
			}
			if (register) {
				concurrentRepository.registerNewSessionInformation(session);
			}
			List<SessionInformation> sessionsExcludeExpired = concurrentRepository.getAllSessions(session, false);
			// exclude itself
			if (sessionsExcludeExpired.size() - 1 == maximumSessions) {
				// valid recently session, expire oldest sessions with same user
				SessionInformation leastRecentlyUsed = null;
				for (SessionInformation si : sessionsExcludeExpired) {
					if (leastRecentlyUsed == null || si.getLastRequest() < leastRecentlyUsed.getLastRequest()) {
						leastRecentlyUsed = si;
					}
				}
				concurrentRepository.expireNow(leastRecentlyUsed.getSessionId());
			}
		}
		filterChain.doFilter(request, response);
	}
}

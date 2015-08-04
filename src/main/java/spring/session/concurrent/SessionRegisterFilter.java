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
@Order(SessionRegisterFilter.DEFAULT_ORDER)
public class SessionRegisterFilter extends OncePerRequestFilter {

	/** after {@link org.springframework.session.web.http.SessionRepositoryFilter#DEFAULT_ORDER} */
	public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 51;

	private SessionRepository sessionRepository;

	private int maximumSessions = 1;

	public SessionRegisterFilter(SessionRepository sessionRepository) {
		if(sessionRepository == null) {
			throw new IllegalArgumentException("sessionRepository cannot be null");
		}
		this.sessionRepository = sessionRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(SessionRepository.VALUE_KEY_PREFIX) != null) {
			sessionRepository.registerNewSessionInformation(
					session.getId(),
					session.getAttribute(SessionRepository.VALUE_KEY_PREFIX).toString()
			);
			List<SessionInformation> sessionsExcludeExpired =
					sessionRepository.getAllSessions(session.getAttribute(SessionRepository.VALUE_KEY_PREFIX).toString(), false);
			// exclude itself
			if (sessionsExcludeExpired.size() - 1 == maximumSessions) {
				// valid recently session, expire oldest sessions with same user
				SessionInformation leastRecentlyUsed = null;
				for (SessionInformation si : sessionsExcludeExpired) {
					if (leastRecentlyUsed == null || si.getLastRequest() < leastRecentlyUsed.getLastRequest()) {
						leastRecentlyUsed = si;
					}
				}
				sessionRepository.expireNow(leastRecentlyUsed.getSessionId());
			}
		}
		filterChain.doFilter(request, response);
	}
}

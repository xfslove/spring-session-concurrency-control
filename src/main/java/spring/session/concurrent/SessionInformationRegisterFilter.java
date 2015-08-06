package spring.session.concurrent;


import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;

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
@Order(SessionInformationRegisterFilter.DEFAULT_ORDER)
public class SessionInformationRegisterFilter extends SessionInformationFilter {

	/** after {@link org.springframework.session.web.http.SessionRepositoryFilter#DEFAULT_ORDER} */
	public static final int DEFAULT_ORDER = SessionRepositoryFilter.DEFAULT_ORDER + 1;

	public SessionInformationRegisterFilter(SessionInformationRepository sessionInformationRepository, ConfigDataProvider configDataProvider) {
		super(sessionInformationRepository, configDataProvider);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(configDataProvider.getPrincipalAttr()) != null) {
			sessionInformationRepository.registerNewSessionInformation(
					session.getId(),
					session.getAttribute(configDataProvider.getPrincipalAttr()).toString()
			);
			List<SessionInformation> sessionsExcludeExpired =
					sessionInformationRepository.getAllSessions(session.getAttribute(configDataProvider.getPrincipalAttr()).toString(), false);
			// exclude itself
			if (sessionsExcludeExpired.size() - 1 == configDataProvider.getMaximumSessions()) {
				// valid recently session, expire oldest sessions with same user
				SessionInformation leastRecentlyUsed = null;
				for (SessionInformation si : sessionsExcludeExpired) {
					if (leastRecentlyUsed == null || si.getLastRequest() < leastRecentlyUsed.getLastRequest()) {
						leastRecentlyUsed = si;
					}
				}
				sessionInformationRepository.expireNow(leastRecentlyUsed.getSessionId());
			}
		}
		filterChain.doFilter(request, response);
	}
}

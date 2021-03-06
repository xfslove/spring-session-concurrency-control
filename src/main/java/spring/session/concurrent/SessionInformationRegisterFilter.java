package spring.session.concurrent;


import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.session.concurrent.ext.MaxSessionCountGetter;
import spring.session.concurrent.ext.PrincipalExistDecider;
import spring.session.concurrent.ext.PrincipalGetter;

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
public class SessionInformationRegisterFilter extends OncePerRequestFilter {

	/** after {@link org.springframework.session.web.http.SessionRepositoryFilter#DEFAULT_ORDER} */
	public static final int DEFAULT_ORDER = SessionRepositoryFilter.DEFAULT_ORDER + 1;

	private SessionInformationRepository sessionInformationRepository;

	private PrincipalGetter principalGetter;

	private MaxSessionCountGetter maxSessionCountGetter;

	private PrincipalExistDecider principalExistDecider;

	public SessionInformationRegisterFilter(SessionInformationRepository sessionInformationRepository, PrincipalGetter principalGetter, MaxSessionCountGetter maxSessionCountGetter, PrincipalExistDecider principalExistDecider) {
		this.sessionInformationRepository = sessionInformationRepository;
		this.principalGetter = principalGetter;
		this.maxSessionCountGetter = maxSessionCountGetter;
		this.principalExistDecider = principalExistDecider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (principalExistDecider.isExistPrincipal(request)) {
			sessionInformationRepository.registerNewSessionInformation(
					session.getId(),
					principalGetter.getPrincipal(request)
			);
			List<SessionInformation> sessionsExcludeExpired =
					sessionInformationRepository.getAllSessions(principalGetter.getPrincipal(request), false);
			// exclude itself
			if (sessionsExcludeExpired.size() - 1 == maxSessionCountGetter.getMaximumSessions(request)) {
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
